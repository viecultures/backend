package com.viecultures.backend.controller;

import com.viecultures.backend.modules.auth.dto.OtpActionResponse;
import com.viecultures.backend.dto.SendEmailNotificationRequest;
import com.viecultures.backend.integration.mail.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/mail")
@RequiredArgsConstructor
@Tag(name = "Automated Email & Notifications", description = "Các API gửi email thông báo hệ thống, bản tin và kiểm tra kết nối Gmail SMTP")
public class MailNotificationController {

    private final EmailService emailService;

    @Operation(summary = "1. Gửi email thông báo tùy chỉnh", description = "Gửi email với template Warm Heritage, hỗ trợ tiêu đề, nội dung HTML, và nút điều hướng CTA.")
    @PostMapping("/send-notification")
    public ResponseEntity<OtpActionResponse> sendNotification(@RequestBody SendEmailNotificationRequest request) {
        if (request.getToEmail() == null || request.getToEmail().isBlank()) {
            return ResponseEntity.badRequest().body(OtpActionResponse.builder()
                    .success(false)
                    .message("Địa chỉ email người nhận không được để trống.")
                    .build());
        }

        emailService.sendNotificationEmail(
                request.getToEmail().trim(),
                request.getSubject() != null ? request.getSubject() : "VieCultures - Thông báo hệ thống",
                request.getHeadline() != null ? request.getHeadline() : "Thông Báo Quan Trọng",
                request.getMessageContent() != null ? request.getMessageContent() : "<p>Bạn có một thông báo mới từ VieCultures.</p>",
                request.getActionUrl() != null ? request.getActionUrl() : "",
                request.getActionText() != null ? request.getActionText() : "Xem Chi Tiết",
                request.getRecipientName()
        );

        return ResponseEntity.ok(OtpActionResponse.builder()
                .success(true)
                .message("Yêu cầu gửi email thông báo đã được đưa vào hàng đợi xử lý.")
                .build());
    }

    @Operation(summary = "2. Kiểm tra kết nối gửi mail (Test SMTP Ping)", description = "Gửi 1 email thử nghiệm nhanh để kiểm tra tính hợp lệ của cấu hình Gmail SMTP và Google App Password.")
    @PostMapping("/test-connection")
    public ResponseEntity<OtpActionResponse> testConnection(@RequestParam String toEmail) {
        if (toEmail == null || toEmail.isBlank()) {
            return ResponseEntity.badRequest().body(OtpActionResponse.builder()
                    .success(false)
                    .message("Vui lòng cung cấp email nhận thử nghiệm.")
                    .build());
        }

        emailService.sendNotificationEmail(
                toEmail.trim(),
                "VieCultures - Thử nghiệm kết nối Gmail SMTP thành công",
                "Kiểm Tra Kết Nối SMTP",
                "<p>Chúc mừng! Tính năng gửi email tự động của <strong>VieCultures</strong> đã được kết nối thành công với Gmail SMTP qua Google App Password.</p><p>Hệ thống hiện đã sẵn sàng phục vụ gửi OTP, xác thực email và gửi thông báo.</p>",
                "https://viecultures.com",
                "Truy Cập Trang Chủ",
                toEmail
        );

        return ResponseEntity.ok(OtpActionResponse.builder()
                .success(true)
                .message("Đã kích hoạt gửi email kiểm tra tới " + toEmail + ". Vui lòng kiểm tra hộp thư.")
                .build());
    }
}
