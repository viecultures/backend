package com.viecultures.backend.service;

import com.viecultures.backend.dto.OtpActionResponse;
import com.viecultures.backend.dto.ResetPasswordRequest;
import com.viecultures.backend.dto.SendOtpRequest;
import com.viecultures.backend.dto.VerifyOtpRequest;
import com.viecultures.backend.entity.EmailOtpToken;
import com.viecultures.backend.entity.OtpPurpose;
import com.viecultures.backend.entity.User;
import com.viecultures.backend.repository.EmailOtpTokenRepository;
import com.viecultures.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final EmailOtpTokenRepository otpTokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private static final int OTP_VALID_MINUTES = 5;
    private static final int COOLDOWN_SECONDS = 60;
    private static final int MAX_ATTEMPTS = 5;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Tạo và gửi mã OTP qua email với cơ chế Rate Limiting (Cooldown 60s)
     */
    @Transactional
    public OtpActionResponse sendOtp(SendOtpRequest request) {
        String email = cleanEmail(request.getEmail());
        OtpPurpose purpose = request.getPurpose() != null ? request.getPurpose() : OtpPurpose.RESET_PASSWORD;

        log.info("Xử lý yêu cầu gửi OTP [Purpose: {}] cho email: {}", purpose, email);

        // Kiểm tra User nếu là yêu cầu RESET_PASSWORD
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (purpose == OtpPurpose.RESET_PASSWORD && userOpt.isEmpty()) {
            return OtpActionResponse.builder()
                    .success(false)
                    .message("Không tìm thấy tài khoản nào gắn với địa chỉ email này.")
                    .cooldownSeconds(0)
                    .build();
        }

        // Kiểm tra Rate Limit (Cooldown 60s)
        Optional<EmailOtpToken> latestTokenOpt = otpTokenRepository.findTopByEmailAndPurposeOrderByIdDesc(email, purpose);
        if (latestTokenOpt.isPresent()) {
            EmailOtpToken latest = latestTokenOpt.get();
            if (latest.getLastRequestedAt() != null) {
                long elapsedSeconds = Duration.between(latest.getLastRequestedAt(), LocalDateTime.now()).getSeconds();
                if (elapsedSeconds < COOLDOWN_SECONDS) {
                    long remainingSeconds = COOLDOWN_SECONDS - elapsedSeconds;
                    return OtpActionResponse.builder()
                            .success(false)
                            .message("Vui lòng đợi " + remainingSeconds + " giây trước khi yêu cầu gửi lại mã OTP.")
                            .cooldownSeconds((int) remainingSeconds)
                            .build();
                }
            }
        }

        // Tạo mã OTP 6 chữ số ngẫu nhiên
        int randomNum = 100000 + secureRandom.nextInt(900000);
        String otpCode = String.valueOf(randomNum);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(OTP_VALID_MINUTES);

        EmailOtpToken token = EmailOtpToken.builder()
                .email(email)
                .otpCode(otpCode)
                .purpose(purpose)
                .expiresAt(expiresAt)
                .attempts(0)
                .verified(false)
                .lastRequestedAt(now)
                .build();

        otpTokenRepository.save(token);

        String recipientName = userOpt.map(User::getName).orElse(email);
        emailService.sendOtpEmail(email, otpCode, purpose, recipientName);

        return OtpActionResponse.builder()
                .success(true)
                .message("Mã OTP đã được gửi về hộp thư của bạn. Vui lòng kiểm tra (kể cả hòm thư Spam).")
                .cooldownSeconds(COOLDOWN_SECONDS)
                .build();
    }

    /**
     * Xác minh tính hợp lệ của mã OTP
     */
    @Transactional
    public OtpActionResponse verifyOtp(VerifyOtpRequest request) {
        String email = cleanEmail(request.getEmail());
        String otpInput = request.getOtpCode() != null ? request.getOtpCode().trim() : "";
        OtpPurpose purpose = request.getPurpose() != null ? request.getPurpose() : OtpPurpose.RESET_PASSWORD;

        Optional<EmailOtpToken> tokenOpt = otpTokenRepository.findTopByEmailAndPurposeOrderByIdDesc(email, purpose);
        if (tokenOpt.isEmpty()) {
            return OtpActionResponse.builder()
                    .success(false)
                    .verified(false)
                    .message("Không tìm thấy yêu cầu OTP hợp lệ. Vui lòng gửi lại mã.")
                    .build();
        }

        EmailOtpToken token = tokenOpt.get();

        if (token.isExpired()) {
            return OtpActionResponse.builder()
                    .success(false)
                    .verified(false)
                    .message("Mã OTP đã hết hạn sau " + OTP_VALID_MINUTES + " phút. Vui lòng gửi yêu cầu mã mới.")
                    .build();
        }

        if (token.isMaxAttemptsExceeded()) {
            return OtpActionResponse.builder()
                    .success(false)
                    .verified(false)
                    .message("Bạn đã nhập sai mã quá " + MAX_ATTEMPTS + " lần. Vui lòng gửi yêu cầu mã OTP mới.")
                    .build();
        }

        if (!token.getOtpCode().equals(otpInput)) {
            token.incrementAttempts();
            otpTokenRepository.save(token);
            int remaining = MAX_ATTEMPTS - token.getAttempts();
            return OtpActionResponse.builder()
                    .success(false)
                    .verified(false)
                    .message("Mã OTP không chính xác. Bạn còn " + Math.max(0, remaining) + " lần thử.")
                    .build();
        }

        token.setVerified(true);
        otpTokenRepository.save(token);

        // Nếu mục đích là xác thực email, cập nhật user ngay
        if (purpose == OtpPurpose.VERIFY_EMAIL) {
            userRepository.findByEmail(email).ifPresent(u -> {
                u.setEmailVerified(true);
                userRepository.save(u);
            });
        }

        return OtpActionResponse.builder()
                .success(true)
                .verified(true)
                .message("Xác thực mã OTP thành công!")
                .build();
    }

    /**
     * Đổi mật khẩu mới sau khi OTP đã được xác minh
     */
    @Transactional
    public OtpActionResponse resetPassword(ResetPasswordRequest request) {
        String email = cleanEmail(request.getEmail());
        String otpInput = request.getOtpCode() != null ? request.getOtpCode().trim() : "";
        String newPassword = request.getNewPassword() != null ? request.getNewPassword().trim() : "";

        if (newPassword.length() < 6) {
            return OtpActionResponse.builder()
                    .success(false)
                    .message("Mật khẩu mới phải có tối thiểu 6 ký tự.")
                    .build();
        }

        Optional<EmailOtpToken> tokenOpt = otpTokenRepository.findTopByEmailAndPurposeOrderByIdDesc(email, OtpPurpose.RESET_PASSWORD);
        if (tokenOpt.isEmpty()) {
            return OtpActionResponse.builder()
                    .success(false)
                    .message("Yêu cầu không hợp lệ. Vui lòng bắt đầu lại từ bước gửi OTP.")
                    .build();
        }

        EmailOtpToken token = tokenOpt.get();

        if (token.isExpired()) {
            return OtpActionResponse.builder()
                    .success(false)
                    .message("Phiên đổi mật khẩu đã hết hạn. Vui lòng gửi lại OTP.")
                    .build();
        }

        if (!token.getOtpCode().equals(otpInput) || !Boolean.TRUE.equals(token.getVerified())) {
            return OtpActionResponse.builder()
                    .success(false)
                    .message("Mã OTP chưa được xác minh hoặc không chính xác.")
                    .build();
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return OtpActionResponse.builder()
                    .success(false)
                    .message("Không tìm thấy tài khoản người dùng.")
                    .build();
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Xóa hoặc vô hiệu hóa OTP để tránh tái sử dụng
        otpTokenRepository.deleteAllByEmailAndPurpose(email, OtpPurpose.RESET_PASSWORD);

        // Gửi email thông báo bảo mật
        emailService.sendPasswordChangedNotification(email, user.getName());

        return OtpActionResponse.builder()
                .success(true)
                .message("Đặt lại mật khẩu thành công! Bạn có thể đăng nhập bằng mật khẩu mới ngay.")
                .build();
    }

    private String cleanEmail(String email) {
        return email != null ? email.trim().toLowerCase() : "";
    }
}
