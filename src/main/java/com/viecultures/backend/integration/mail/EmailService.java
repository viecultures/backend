package com.viecultures.backend.integration.mail;

import com.viecultures.backend.entity.OtpPurpose;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:viecultures@gmail.com}")
    private String fromEmail;

    @Value("${mail.from.name:VieCultures Team}")
    private String fromName;

    /**
     * Gửi email chứa mã OTP (Bất đồng bộ - Async)
     */
    @Async("mailTaskExecutor")
    public void sendOtpEmail(String toEmail, String otpCode, OtpPurpose purpose, String recipientName) {
        try {
            log.info("📧 Bắt đầu gửi email OTP [{}] đến: {}", purpose, toEmail);

            String subject;
            String headline;
            String purposeDescription;

            if (purpose == OtpPurpose.RESET_PASSWORD) {
                subject = "VieCultures - Mã OTP đặt lại mật khẩu";
                headline = "Đặt Lại Mật Khẩu";
                purposeDescription = "Bạn vừa gửi yêu cầu đặt lại mật khẩu cho tài khoản VieCultures của mình. Vui lòng nhập mã OTP sau để tiếp tục thiết lập mật khẩu mới:";
            } else if (purpose == OtpPurpose.VERIFY_EMAIL) {
                subject = "VieCultures - Mã OTP xác thực địa chỉ email";
                headline = "Xác Thực Địa Chỉ Email";
                purposeDescription = "Cảm ơn bạn đã đồng hành cùng VieCultures. Vui lòng sử dụng mã OTP dưới đây để hoàn tất bước xác thực email của bạn:";
            } else {
                subject = "VieCultures - Mã xác thực bảo mật";
                headline = "Mã Xác Thực";
                purposeDescription = "Hệ thống VieCultures gửi đến bạn mã xác thực bảo mật:";
            }

            Context context = new Context();
            context.setVariable("subject", subject);
            context.setVariable("headline", headline);
            context.setVariable("recipientName", recipientName != null && !recipientName.isBlank() ? recipientName : toEmail);
            context.setVariable("purposeDescription", purposeDescription);
            context.setVariable("otpCode", otpCode);
            context.setVariable("validMinutes", 5);

            String htmlBody = templateEngine.process("email-otp", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("✅ Gửi email OTP [{}] thành công tới {}", purpose, toEmail);

        } catch (Exception ex) {
            log.error("❌ Lỗi khi gửi email OTP tới {}: {}", toEmail, ex.getMessage(), ex);
        }
    }

    /**
     * Gửi email cảnh báo bảo mật khi mật khẩu đã được đổi thành công
     */
    @Async("mailTaskExecutor")
    public void sendPasswordChangedNotification(String toEmail, String recipientName) {
        try {
            String subject = "VieCultures - Mật khẩu của bạn đã được thay đổi";
            String headline = "Mật Khẩu Đã Thay Đổi Thành Công";
            String message = "<p>Mật khẩu cho tài khoản của bạn tại <strong>VieCultures</strong> vừa được thay đổi thành công.</p>" +
                    "<p>Nếu bạn là người thực hiện thay đổi này, bạn có thể an tâm đăng nhập tiếp tục trải nghiệm bài học.</p>" +
                    "<p style='color: #C53030;'><strong>Nếu bạn KHÔNG thực hiện thay đổi này</strong>, tài khoản của bạn có thể đã bị truy cập trái phép. Vui lòng liên hệ ngay với đội ngũ hỗ trợ VieCultures để được bảo vệ.</p>";

            sendNotificationEmail(toEmail, subject, headline, message, "", "", recipientName);
        } catch (Exception ex) {
            log.error("❌ Lỗi khi gửi email thông báo đổi mật khẩu: {}", ex.getMessage());
        }
    }

    /**
     * Gửi email thông báo bất kỳ (HTML Template Warm Heritage)
     */
    @Async("mailTaskExecutor")
    public void sendNotificationEmail(String toEmail, String subject, String headline, String messageHtml, String actionUrl, String actionText, String recipientName) {
        try {
            log.info("📧 Bắt đầu gửi email thông báo [{}] đến: {}", subject, toEmail);

            Context context = new Context();
            context.setVariable("subject", subject);
            context.setVariable("headline", headline);
            context.setVariable("recipientName", recipientName != null && !recipientName.isBlank() ? recipientName : toEmail);
            context.setVariable("messageContent", messageHtml);
            context.setVariable("actionUrl", actionUrl);
            context.setVariable("actionText", actionText);

            String htmlBody = templateEngine.process("email-notification", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("✅ Gửi email thông báo thành công tới {}", toEmail);

        } catch (Exception ex) {
            log.error("❌ Lỗi khi gửi email thông báo tới {}: {}", toEmail, ex.getMessage(), ex);
        }
    }
}
