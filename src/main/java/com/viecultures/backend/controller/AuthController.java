package com.viecultures.backend.controller;

import com.viecultures.backend.dto.*;
import com.viecultures.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
@RequiredArgsConstructor
@Tag(name = "4. Authentication & User Access", description = "Đăng nhập tài khoản thường, Google OAuth, Đăng ký và Quản lý phiên làm việc JWT")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Đăng nhập bằng Email & Mật khẩu",
            description = "Xác thực tài khoản và trả về JWT Bearer token kèm thông tin hồ sơ người dùng"
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.ok(response, "Đăng nhập thành công"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(ex.getMessage(), "BAD_CREDENTIALS"));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(ex.getMessage(), "ACCOUNT_DISABLED"));
        }
    }

    @Operation(
            summary = "Đăng nhập hoặc liên kết bằng Google OAuth (Web/Mobile)",
            description = "Nhận Google ID Token hoặc thông tin profile từ Google Sign-In, tự động kích hoạt tài khoản Just-In-Time và trả về JWT Bearer token"
    )
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        try {
            AuthResponse response = authService.loginWithGoogle(request);
            return ResponseEntity.ok(ApiResponse.ok(response, "Đăng nhập Google thành công"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(ex.getMessage(), "GOOGLE_AUTH_FAILED"));
        } catch (Exception ex) {
            log.error("Google authentication error: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi xác thực tài khoản Google: " + ex.getMessage(), "SERVER_ERROR"));
        }
    }

    @Operation(
            summary = "Đăng ký tài khoản người học mới",
            description = "Tạo tài khoản người dùng mới với mật khẩu mã hóa BCrypt và tự động cấp JWT phiên đầu tiên"
    )
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(response, "Đăng ký tài khoản thành công"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(ex.getMessage(), "EMAIL_ALREADY_EXISTS"));
        }
    }

    @Operation(
            summary = "Lấy thông tin tài khoản hiện tại (Me)",
            description = "Truy xuất hồ sơ cá nhân của người dùng đang đăng nhập dựa trên JWT token được gửi kèm trong header Authorization: Bearer <token>"
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse.UserInfo>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Bạn chưa đăng nhập hoặc token không hợp lệ", "UNAUTHORIZED"));
        }

        try {
            AuthResponse.UserInfo userInfo = authService.getCurrentUser(userDetails.getUsername());
            return ResponseEntity.ok(ApiResponse.ok(userInfo));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(ex.getMessage(), "USER_NOT_FOUND"));
        }
    }
}
