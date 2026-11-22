package com.viecultures.backend.integration.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.viecultures.backend.common.security.JwtService;
import com.viecultures.backend.modules.auth.dto.AuthResponse;
import com.viecultures.backend.modules.auth.dto.AuthUserDto;
import com.viecultures.backend.modules.auth.dto.GoogleLoginRequest;
import com.viecultures.backend.entity.CefrLevel;
import com.viecultures.backend.entity.User;
import com.viecultures.backend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${google.client-id}")
    private String googleClientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() {
        try {
            this.verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
            log.info("GoogleIdTokenVerifier initialized with clientId: {}", googleClientId);
        } catch (GeneralSecurityException | IOException e) {
            log.error("Failed to initialize GoogleIdTokenVerifier: {}", e.getMessage(), e);
            throw new IllegalStateException("Không thể khởi tạo GoogleIdTokenVerifier: " + e.getMessage(), e);
        }
    }

    /**
     * Xác thực Google ID Token, tự động provisioning người dùng và trả về
     * AuthResponse kèm JWT.
     *
     * @param request chứa idToken gửi từ phía frontend
     * @return AuthResponse gồm token, tokenType, expiresIn, AuthUserDto
     */
    @Transactional
    public AuthResponse authenticateGoogle(GoogleLoginRequest request) {
        if (request == null || request.getIdToken() == null || request.getIdToken().isBlank()) {
            throw new IllegalArgumentException("Invalid Google ID token");
        }

        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(request.getIdToken().trim());
        } catch (Exception e) {
            log.error("Error verifying Google ID token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid Google ID token: " + e.getMessage());
        }

        if (idToken == null) {
            throw new IllegalArgumentException("Invalid Google ID token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Google token does not contain a valid email");
        }
        email = email.trim().toLowerCase();

        String name = (String) payload.get("name");
        if (name == null || name.isBlank()) {
            name = email.split("@")[0];
        }
        String picture = (String) payload.get("picture");
        Boolean emailVerified = payload.getEmailVerified();
        if (emailVerified == null) {
            emailVerified = true;
        }
        String googleId = payload.getSubject();

        Optional<User> optionalUser = userRepository.findByEmail(email);
        User user;

        if (optionalUser.isEmpty()) {
            // Nếu chưa có user: tạo mới User
            user = User.builder()
                    .email(email)
                    .name(name)
                    .avatarUrl(picture)
                    .googleId(googleId)
                    .authProvider("GOOGLE")
                    .role("STUDENT")
                    .emailVerified(emailVerified)
                    .enabled(true)
                    .englishLevel(CefrLevel.A1)
                    .build();
            user = userRepository.save(user);
            log.info("Provisioned new Google user: id={}, email={}", user.getId(), user.getEmail());
        } else {
            // Nếu đã có user: cập nhật avatarUrl nếu user chưa có avatar
            user = optionalUser.get();
            boolean updated = false;

            if (picture != null && !picture.isBlank()
                    && (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank())) {
                user.setAvatarUrl(picture);
                updated = true;
            }
            if (googleId != null && (user.getGoogleId() == null || !user.getGoogleId().equals(googleId))) {
                user.setGoogleId(googleId);
                updated = true;
            }
            if (Boolean.TRUE.equals(emailVerified) && !Boolean.TRUE.equals(user.getEmailVerified())) {
                user.setEmailVerified(true);
                updated = true;
            }
            if (updated) {
                user = userRepository.save(user);
                log.info("Updated existing user avatar/info from Google: id={}, email={}", user.getId(),
                        user.getEmail());
            }
        }

        // Sinh VieCultures JWT token
        String token = jwtService.generateToken(user);

        AuthUserDto userDto = AuthUserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .englishLevel(user.getEnglishLevel())
                .build();

        return AuthResponse.builder()
                .token(token)
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs())
                .user(userDto)
                .build();
    }
}
