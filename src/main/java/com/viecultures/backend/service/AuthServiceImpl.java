package com.viecultures.backend.service;

import com.viecultures.backend.dto.AuthRequest;
import com.viecultures.backend.dto.AuthResponse;
import com.viecultures.backend.dto.AuthUserDto;
import com.viecultures.backend.dto.GoogleLoginRequest;
import com.viecultures.backend.dto.RegisterRequest;
import com.viecultures.backend.entity.CefrLevel;
import com.viecultures.backend.entity.User;
import com.viecultures.backend.repository.UserRepository;
import com.viecultures.backend.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RestTemplate restTemplate;

    private static final String GOOGLE_TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        log.info("Attempting login for email: {}", request.getEmail());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail().trim().toLowerCase(),
                            request.getPassword()));
        } catch (BadCredentialsException ex) {
            throw new IllegalArgumentException("Email hoặc mật khẩu không chính xác");
        }

        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy người dùng với email: " + request.getEmail()));

        if (!user.isEnabled()) {
            throw new IllegalStateException("Tài khoản đã bị tạm khóa hoặc chưa được kích hoạt");
        }

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Registering new user with email: {}", email);

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã được đăng ký trong hệ thống");
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName().trim())
                .role("ROLE_USER")
                .authProvider("LOCAL")
                .enabled(true)
                .englishLevel(request.getEnglishLevel() != null ? request.getEnglishLevel() : CefrLevel.B1)
                .build();

        User savedUser = userRepository.save(user);
        return buildAuthResponse(savedUser);
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        String email = null;
        String name = null;
        String avatarUrl = null;
        String googleId = null;

        // 1. Xác thực Google ID Token với Google Endpoint nếu có idToken
        if (request.getIdToken() != null && !request.getIdToken().isBlank()) {
            try {
                String verifyUrl = GOOGLE_TOKENINFO_URL + request.getIdToken().trim();
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        verifyUrl,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {
                        });

                Map<String, Object> body = response.getBody();
                if (body != null && body.containsKey("email")) {
                    email = (String) body.get("email");
                    googleId = (String) body.get("sub");
                    name = (String) body.get("name");
                    avatarUrl = (String) body.get("picture");
                    log.info("Google token validated successfully for email: {}, googleId: {}", email, googleId);
                } else {
                    throw new IllegalArgumentException("Google ID Token không chứa thông tin email hợp lệ");
                }
            } catch (Exception e) {
                log.error("Google token verification failed: {}", e.getMessage());
                // Nếu verify thất bại nhưng client gửi profile trực tiếp (chế độ dev /
                // fallback)
                if (request.getEmail() == null || request.getEmail().isBlank()) {
                    throw new IllegalArgumentException(
                            "Mã xác thực Google không hợp lệ hoặc đã hết hạn: " + e.getMessage());
                }
            }
        }

        // 2. Sử dụng fallback trực tiếp từ request payload nếu tokeninfo không giải mã
        // được
        if (email == null && request.getEmail() != null && !request.getEmail().isBlank()) {
            email = request.getEmail().trim().toLowerCase();
            name = request.getName();
            avatarUrl = request.getAvatarUrl();
            googleId = request.getGoogleId();
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Vui lòng cung cấp Google idToken hoặc email hợp lệ");
        }

        email = email.trim().toLowerCase();

        // 3. Tìm hoặc tự động tạo tài khoản người dùng (Just-In-Time Provisioning)
        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        if (existingUserOpt.isEmpty() && googleId != null && !googleId.isBlank()) {
            existingUserOpt = userRepository.findByGoogleId(googleId);
        }

        User user;
        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            // Cập nhật thông tin bổ sung nếu người dùng cập nhật avatar hoặc tên từ Google
            boolean updated = false;
            if (googleId != null && (user.getGoogleId() == null || !user.getGoogleId().equals(googleId))) {
                user.setGoogleId(googleId);
                updated = true;
            }
            if (avatarUrl != null && (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank())) {
                user.setAvatarUrl(avatarUrl);
                updated = true;
            }
            if (name != null && !name.isBlank() && (user.getName() == null || user.getName().isBlank())) {
                user.setName(name);
                updated = true;
            }
            if (updated) {
                user = userRepository.save(user);
            }
        } else {
            // Tạo tài khoản mới cho người dùng Google
            String displayName = (name != null && !name.isBlank()) ? name : email.split("@")[0];
            user = User.builder()
                    .email(email)
                    .name(displayName)
                    .avatarUrl(avatarUrl)
                    .googleId(googleId)
                    .authProvider("GOOGLE")
                    .role("ROLE_USER")
                    .enabled(true)
                    .englishLevel(CefrLevel.B1)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .build();
            user = userRepository.save(user);
            log.info("Provisioned new Google user: id={}, email={}", user.getId(), user.getEmail());
        }

        return buildAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse.UserInfo getCurrentUser(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng: " + email));

        return toUserInfo(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        Map<String, Object> extraClaims = Map.of(
                "userId", user.getId(),
                "role", user.getRole() != null ? user.getRole() : "STUDENT",
                "name", user.getName(),
                "authProvider", user.getAuthProvider() != null ? user.getAuthProvider() : "LOCAL");

        String token = jwtService.generateToken(extraClaims, user);

        return AuthResponse.builder()
                .token(token)
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs())
                .user(toAuthUserDto(user))
                .build();
    }

    private AuthUserDto toAuthUserDto(User user) {
        return AuthUserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .englishLevel(user.getEnglishLevel())
                .build();
    }

    private AuthResponse.UserInfo toUserInfo(User user) {
        return AuthResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .authProvider(user.getAuthProvider())
                .englishLevel(user.getEnglishLevel())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
