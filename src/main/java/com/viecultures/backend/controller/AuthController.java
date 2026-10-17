package com.viecultures.backend.controller;

import com.viecultures.backend.dto.OtpActionResponse;
import com.viecultures.backend.dto.ResetPasswordRequest;
import com.viecultures.backend.dto.SendOtpRequest;
import com.viecultures.backend.dto.VerifyOtpRequest;
import com.viecultures.backend.entity.OtpPurpose;
import com.viecultures.backend.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & OTP Security", description = "Các API phục vụ xác thực bảo mật, OTP qua email và đổi mật khẩu")
public class AuthController {

    private final OtpService otpService;

    @Operation(summary = "1. Gửi mã OTP đặt lại mật khẩu", description = "Kiểm tra email và gửi mã OTP 6 số qua Gmail SMTP. Giới hạn gửi lại sau 60 giây.")
    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<OtpActionResponse> sendForgotPasswordOtp(@RequestBody SendOtpRequest request) {
        request.setPurpose(OtpPurpose.RESET_PASSWORD);
        OtpActionResponse response = otpService.sendOtp(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "2. Xác minh mã OTP", description = "Kiểm tra mã OTP người dùng nhập vào. Cho phép tối đa 5 lần nhập sai trước khi khóa mã.")
    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<OtpActionResponse> verifyForgotPasswordOtp(@RequestBody VerifyOtpRequest request) {
        request.setPurpose(OtpPurpose.RESET_PASSWORD);
        OtpActionResponse response = otpService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "3. Thiết lập mật khẩu mới", description = "Cập nhật mật khẩu mới (mã hóa BCrypt) khi mã OTP đã được xác minh thành công.")
    @PostMapping("/forgot-password/reset")
    public ResponseEntity<OtpActionResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        OtpActionResponse response = otpService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "4. Gửi mã OTP xác thực email tài khoản", description = "Gửi mã kích hoạt/xác minh quyền sở hữu email qua Gmail SMTP.")
    @PostMapping("/verify-email/send")
    public ResponseEntity<OtpActionResponse> sendVerifyEmailOtp(@RequestBody SendOtpRequest request) {
        request.setPurpose(OtpPurpose.VERIFY_EMAIL);
        OtpActionResponse response = otpService.sendOtp(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "5. Xác nhận địa chỉ email", description = "Kiểm tra OTP và đánh dấu tài khoản đã được xác thực email (email_verified = true).")
    @PostMapping("/verify-email/confirm")
    public ResponseEntity<OtpActionResponse> confirmEmail(@RequestBody VerifyOtpRequest request) {
        request.setPurpose(OtpPurpose.VERIFY_EMAIL);
        OtpActionResponse response = otpService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }
}
