package com.viecultures.backend.controller;

import com.viecultures.backend.common.response.ApiResponse;
import com.viecultures.backend.dto.*;
import com.viecultures.backend.entity.CefrLevel;
import com.viecultures.backend.service.AuthService;
import com.viecultures.backend.integration.google.GoogleAuthService;
import com.viecultures.backend.service.OtpService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private GoogleAuthService googleAuthService;

    @Mock
    private OtpService otpService;

    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("Login thành công trả về 200 OK và JWT Token")
    void testLoginSuccess() {
        AuthRequest request = AuthRequest.builder()
                .email("test@viecultures.com")
                .password("Password123@")
                .build();

        AuthResponse mockResponse = AuthResponse.builder()
                .accessToken("mock-jwt-token")
                .tokenType("Bearer")
                .expiresIn(604800000L)
                .user(AuthUserDto.builder()
                        .email("test@viecultures.com")
                        .name("Test User")
                        .role("ROLE_USER")
                        .build())
                .build();

        when(authService.login(any(AuthRequest.class))).thenReturn(mockResponse);

        ResponseEntity<ApiResponse<AuthResponse>> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("mock-jwt-token", response.getBody().getData().getAccessToken());
    }

    @Test
    @DisplayName("Login sai mật khẩu trả về 401 UNAUTHORIZED")
    void testLoginBadCredentials() {
        AuthRequest request = AuthRequest.builder()
                .email("test@viecultures.com")
                .password("WrongPassword")
                .build();

        when(authService.login(any(AuthRequest.class)))
                .thenThrow(new IllegalArgumentException("Email hoặc mật khẩu không chính xác"));

        ResponseEntity<ApiResponse<AuthResponse>> response = authController.login(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("BAD_CREDENTIALS", response.getBody().getErrorCode());
    }

    @Test
    @DisplayName("Google Login thành công trả về 200 OK và JWT Token")
    void testGoogleLoginSuccess() {
        GoogleLoginRequest request = GoogleLoginRequest.builder()
                .idToken("mock-google-id-token")
                .email("google.user@gmail.com")
                .name("Google Learner")
                .build();

        AuthResponse mockResponse = AuthResponse.builder()
                .token("mock-google-jwt-token")
                .accessToken("mock-google-jwt-token")
                .tokenType("Bearer")
                .expiresIn(604800000L)
                .user(AuthUserDto.builder()
                        .email("google.user@gmail.com")
                        .name("Google Learner")
                        .authProvider("GOOGLE")
                        .role("STUDENT")
                        .build())
                .build();

        when(googleAuthService.authenticateGoogle(any(GoogleLoginRequest.class))).thenReturn(mockResponse);

        ResponseEntity<ApiResponse<AuthResponse>> response = authController.loginWithGoogle(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("GOOGLE", response.getBody().getData().getUser().getAuthProvider());
    }

    @Test
    @DisplayName("Đăng ký người dùng mới thành công trả về 201 CREATED")
    void testRegisterSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .email("new@viecultures.com")
                .password("Password123@")
                .name("New Learner")
                .englishLevel(CefrLevel.B1)
                .build();

        AuthResponse mockResponse = AuthResponse.builder()
                .accessToken("mock-register-jwt-token")
                .user(AuthUserDto.builder()
                        .email("new@viecultures.com")
                        .name("New Learner")
                        .build())
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(mockResponse);

        ResponseEntity<ApiResponse<AuthResponse>> response = authController.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    @DisplayName("Get Current User (/me) trả về thông tin người dùng đang đăng nhập")
    void testGetCurrentUserSuccess() {
        UserDetails userDetails = new User("user@viecultures.com", "pass", Collections.emptyList());

        AuthResponse.UserInfo mockUserInfo = AuthResponse.UserInfo.builder()
                .email("user@viecultures.com")
                .name("Current User")
                .role("ROLE_USER")
                .build();

        when(authService.getCurrentUser("user@viecultures.com")).thenReturn(mockUserInfo);

        ResponseEntity<ApiResponse<AuthResponse.UserInfo>> response = authController.getCurrentUser(userDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("user@viecultures.com", response.getBody().getData().getEmail());
    }
}
