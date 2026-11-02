package com.viecultures.backend.service;

import com.viecultures.backend.dto.AuthRequest;
import com.viecultures.backend.dto.AuthResponse;
import com.viecultures.backend.dto.RegisterRequest;
import com.viecultures.backend.dto.SendOtpRequest;
import com.viecultures.backend.entity.CefrLevel;
import com.viecultures.backend.entity.OtpPurpose;
import com.viecultures.backend.entity.User;
import com.viecultures.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private OtpService otpService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User activeUser;
    private User disabledUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id("user-uuid-1")
                .email("learner@viecultures.com")
                .password("encoded_password")
                .name("Nguyen Van A")
                .role("ROLE_USER")
                .authProvider("LOCAL")
                .enabled(true)
                .emailVerified(true)
                .englishLevel(CefrLevel.B1)
                .createdAt(LocalDateTime.now())
                .build();

        disabledUser = User.builder()
                .id("user-uuid-2")
                .email("disabled@viecultures.com")
                .password("encoded_password")
                .name("Nguyen Van B")
                .role("ROLE_USER")
                .authProvider("LOCAL")
                .enabled(false)
                .emailVerified(false)
                .englishLevel(CefrLevel.A2)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("login_Success: Thong tin hop le -> Tra ve token va User info")
    void login_Success() {
        AuthRequest request = AuthRequest.builder()
                .email("learner@viecultures.com")
                .password("RawPassword123")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmail("learner@viecultures.com")).thenReturn(Optional.of(activeUser));
        when(jwtService.generateToken(anyMap(), eq(activeUser))).thenReturn("mocked-jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(604800000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mocked-jwt-token", response.getToken());
        assertEquals("mocked-jwt-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(604800000L, response.getExpiresIn());
        assertNotNull(response.getUser());
        assertEquals("learner@viecultures.com", response.getUser().getEmail());
        assertEquals("Nguyen Van A", response.getUser().getName());
        assertEquals("ROLE_USER", response.getUser().getRole());
        assertEquals(CefrLevel.B1, response.getUser().getEnglishLevel());

        ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(authCaptor.capture());
        assertEquals("learner@viecultures.com", authCaptor.getValue().getPrincipal());
        assertEquals("RawPassword123", authCaptor.getValue().getCredentials());
        verify(userRepository).findByEmail("learner@viecultures.com");
        verify(jwtService).generateToken(anyMap(), eq(activeUser));
    }

    @Test
    @DisplayName("login_BadCredentials: Sai mat khau -> Nem IllegalArgumentException")
    void login_BadCredentials() {
        AuthRequest request = AuthRequest.builder()
                .email("learner@viecultures.com")
                .password("WrongPassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.login(request));

        assertEquals("Email hoặc mật khẩu không chính xác", exception.getMessage());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByEmail(anyString());
        verify(jwtService, never()).generateToken(anyMap(), any(User.class));
    }

    @Test
    @DisplayName("login_DisabledUser: Tai khoan chua kich hoat hoac disabled -> Nem IllegalStateException")
    void login_DisabledUser() {
        AuthRequest request = AuthRequest.builder()
                .email("disabled@viecultures.com")
                .password("RawPassword123")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmail("disabled@viecultures.com")).thenReturn(Optional.of(disabledUser));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> authService.login(request));

        assertEquals("Tài khoản đã bị tạm khóa hoặc chưa được kích hoạt", exception.getMessage());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmail("disabled@viecultures.com");
        verify(jwtService, never()).generateToken(anyMap(), any(User.class));
    }

    @Test
    @DisplayName("login_UserNotFound: Tim user khong thay -> Nem IllegalArgumentException")
    void login_UserNotFound() {
        AuthRequest request = AuthRequest.builder()
                .email("notfound@viecultures.com")
                .password("RawPassword123")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmail("notfound@viecultures.com")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.login(request));

        assertTrue(exception.getMessage().contains("Không tìm thấy người dùng với email: notfound@viecultures.com"));
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmail("notfound@viecultures.com");
        verify(jwtService, never()).generateToken(anyMap(), any(User.class));
    }

    @Test
    @DisplayName("register_Success: Email moi -> Luu user ma hoa password, goi otpService.sendOtp, tra ve auth response")
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .email("NewUser@viecultures.com ")
                .password("NewPassword123")
                .name("Tran Van C")
                .englishLevel(CefrLevel.B2)
                .build();

        String normalizedEmail = "newuser@viecultures.com";

        when(userRepository.existsByEmail(normalizedEmail)).thenReturn(false);
        when(passwordEncoder.encode("NewPassword123")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId("user-uuid-3");
            return saved;
        });
        when(jwtService.generateToken(anyMap(), any(User.class))).thenReturn("mocked-jwt-token-registered");
        when(jwtService.getExpirationMs()).thenReturn(604800000L);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mocked-jwt-token-registered", response.getToken());
        assertEquals(604800000L, response.getExpiresIn());
        assertNotNull(response.getUser());
        assertEquals(normalizedEmail, response.getUser().getEmail());
        assertEquals("Tran Van C", response.getUser().getName());
        assertEquals(CefrLevel.B2, response.getUser().getEnglishLevel());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertEquals(normalizedEmail, capturedUser.getEmail());
        assertEquals("encodedNewPassword", capturedUser.getPassword());
        assertEquals("Tran Van C", capturedUser.getName());
        assertEquals("ROLE_USER", capturedUser.getRole());
        assertEquals("LOCAL", capturedUser.getAuthProvider());
        assertTrue(capturedUser.isEnabled());
        assertFalse(capturedUser.getEmailVerified());
        assertEquals(CefrLevel.B2, capturedUser.getEnglishLevel());

        ArgumentCaptor<SendOtpRequest> otpCaptor = ArgumentCaptor.forClass(SendOtpRequest.class);
        verify(otpService).sendOtp(otpCaptor.capture());
        SendOtpRequest capturedOtpRequest = otpCaptor.getValue();
        assertEquals(normalizedEmail, capturedOtpRequest.getEmail());
        assertEquals(OtpPurpose.VERIFY_EMAIL, capturedOtpRequest.getPurpose());
    }

    @Test
    @DisplayName("register_EmailAlreadyExists: Email da ton tai -> Nem IllegalArgumentException")
    void register_EmailAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .email("learner@viecultures.com")
                .password("Password123")
                .name("Nguyen Van A")
                .englishLevel(CefrLevel.B1)
                .build();

        when(userRepository.existsByEmail("learner@viecultures.com")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.register(request));

        assertEquals("Email đã được đăng ký trong hệ thống", exception.getMessage());
        verify(userRepository).existsByEmail("learner@viecultures.com");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(otpService, never()).sendOtp(any(SendOtpRequest.class));
    }
}
