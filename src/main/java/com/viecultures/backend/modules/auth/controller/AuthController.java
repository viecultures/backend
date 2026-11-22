package com.viecultures.backend.modules.auth.controller;

import com.viecultures.backend.common.response.ApiResponse;
import com.viecultures.backend.dto.*;
import com.viecultures.backend.modules.auth.dto.*;
import com.viecultures.backend.entity.OtpPurpose;
import com.viecultures.backend.modules.auth.service.AuthService;
import com.viecultures.backend.integration.google.GoogleAuthService;
import com.viecultures.backend.modules.auth.service.OtpService;
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
@Tag(name = "4. Authentication & User Access", description = "Đăng nhập tài khoản thường, Google OAuth, Đăng ký, Quản lý phiên JWT và Xác thực bảo mật OTP qua Email")
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;
    private final OtpService otpService;

    // ─── Core Authentication & OAuth ───────────────────────────────────────────

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

    @Operation(summary = "Đăng nhập bằng Google ID Token")
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        try {
            AuthResponse response = googleAuthService.authenticateGoogle(request);
            return ResponseEntity.ok(ApiResponse.success(response, "Đăng nhập Google thành công"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(ex.getMessage()));
        } catch (Exception ex) {
            log.error("Google authentication error: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi xác thực tài khoản Google: " + ex.getMessage()));
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

    // ─── Automated Email & OTP Password Reset ─────────────────────────────────

    @Operation(summary = "Gửi mã OTP đặt lại mật khẩu", description = "Kiểm tra email và gửi mã OTP 6 số qua Gmail SMTP. Giới hạn gửi lại sau 60 giây.")
    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<OtpActionResponse> sendForgotPasswordOtp(@RequestBody SendOtpRequest request) {
        request.setPurpose(OtpPurpose.RESET_PASSWORD);
        OtpActionResponse response = otpService.sendOtp(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Xác minh mã OTP", description = "Kiểm tra mã OTP người dùng nhập vào. Cho phép tối đa 5 lần nhập sai trước khi khóa mã.")
    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<OtpActionResponse> verifyForgotPasswordOtp(@RequestBody VerifyOtpRequest request) {
        request.setPurpose(OtpPurpose.RESET_PASSWORD);
        OtpActionResponse response = otpService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Thiết lập mật khẩu mới", description = "Cập nhật mật khẩu mới (mã hóa BCrypt) khi mã OTP đã được xác minh thành công.")
    @PostMapping("/forgot-password/reset")
    public ResponseEntity<OtpActionResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        OtpActionResponse response = otpService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Gửi mã OTP xác thực email tài khoản", description = "Gửi mã kích hoạt/xác minh quyền sở hữu email qua Gmail SMTP.")
    @PostMapping("/verify-email/send")
    public ResponseEntity<OtpActionResponse> sendVerifyEmailOtp(@RequestBody SendOtpRequest request) {
        request.setPurpose(OtpPurpose.VERIFY_EMAIL);
        OtpActionResponse response = otpService.sendOtp(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Xác nhận địa chỉ email", description = "Kiểm tra OTP và đánh dấu tài khoản đã được xác thực email (email_verified = true).")
    @PostMapping("/verify-email/confirm")
    public ResponseEntity<OtpActionResponse> confirmEmail(@RequestBody VerifyOtpRequest request) {
        request.setPurpose(OtpPurpose.VERIFY_EMAIL);
        OtpActionResponse response = otpService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }
}
