package com.viecultures.backend.service;

import com.viecultures.backend.dto.OtpActionResponse;
import com.viecultures.backend.dto.ResetPasswordRequest;
import com.viecultures.backend.dto.SendOtpRequest;
import com.viecultures.backend.dto.VerifyOtpRequest;
import com.viecultures.backend.entity.EmailOtpToken;
import com.viecultures.backend.entity.OtpPurpose;
import com.viecultures.backend.entity.User;
import com.viecultures.backend.integration.mail.EmailService;
import com.viecultures.backend.repository.EmailOtpTokenRepository;
import com.viecultures.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private EmailOtpTokenRepository otpTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OtpService otpService;

    private User sampleUser;
    private final String sampleEmail = "learner@viecultures.com";

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id("user-uuid-101")
                .email(sampleEmail)
                .name("Nguyen Van A")
                .password("encoded_old_password")
                .role("ROLE_USER")
                .enabled(true)
                .emailVerified(false)
                .build();
    }

    @Test
    @DisplayName("sendOtp_Success: Gui OTP thanh cong -> Tao ma, luu vao DB, goi emailService va tra ve success=true")
    void sendOtp_Success() {
        SendOtpRequest request = SendOtpRequest.builder()
                .email(sampleEmail)
                .purpose(OtpPurpose.RESET_PASSWORD)
                .build();

        when(userRepository.findByEmail(sampleEmail)).thenReturn(Optional.of(sampleUser));
        when(otpTokenRepository.findTopByEmailAndPurposeOrderByIdDesc(sampleEmail, OtpPurpose.RESET_PASSWORD))
                .thenReturn(Optional.empty());

        OtpActionResponse response = otpService.sendOtp(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(60, response.getCooldownSeconds());
        assertTrue(response.getMessage().contains("Mã OTP đã được gửi về hộp thư của bạn"));

        ArgumentCaptor<EmailOtpToken> tokenCaptor = ArgumentCaptor.forClass(EmailOtpToken.class);
        verify(otpTokenRepository, times(1)).save(tokenCaptor.capture());
        EmailOtpToken capturedToken = tokenCaptor.getValue();
        assertEquals(sampleEmail, capturedToken.getEmail());
        assertEquals(OtpPurpose.RESET_PASSWORD, capturedToken.getPurpose());
        assertNotNull(capturedToken.getOtpCode());
        assertEquals(6, capturedToken.getOtpCode().length());
        assertEquals(0, capturedToken.getAttempts());
        assertFalse(capturedToken.getVerified());
        assertNotNull(capturedToken.getExpiresAt());
        assertNotNull(capturedToken.getLastRequestedAt());

        verify(emailService, times(1)).sendOtpEmail(
                eq(sampleEmail),
                eq(capturedToken.getOtpCode()),
                eq(OtpPurpose.RESET_PASSWORD),
                eq(sampleUser.getName())
        );
    }

    @Test
    @DisplayName("sendOtp_Success_VerifyEmailPurpose: Gui OTP voi muc dich VERIFY_EMAIL thanh cong")
    void sendOtp_Success_VerifyEmailPurpose() {
        SendOtpRequest request = SendOtpRequest.builder()
                .email(sampleEmail)
                .purpose(OtpPurpose.VERIFY_EMAIL)
                .build();

        when(userRepository.findByEmail(sampleEmail)).thenReturn(Optional.of(sampleUser));
        when(otpTokenRepository.findTopByEmailAndPurposeOrderByIdDesc(sampleEmail, OtpPurpose.VERIFY_EMAIL))
                .thenReturn(Optional.empty());

        OtpActionResponse response = otpService.sendOtp(request);

        assertTrue(response.isSuccess());
        assertEquals(60, response.getCooldownSeconds());
        verify(otpTokenRepository, times(1)).save(any(EmailOtpToken.class));
        verify(emailService, times(1)).sendOtpEmail(
                eq(sampleEmail),
                anyString(),
                eq(OtpPurpose.VERIFY_EMAIL),
                eq(sampleUser.getName())
        );
    }

    @Test
    @DisplayName("sendOtp_CooldownActive: Yeu cau gui lai khi chua het 60s cooldown -> Tra ve success=false kem so giay con lai")
    void sendOtp_CooldownActive() {
        SendOtpRequest request = SendOtpRequest.builder()
                .email(sampleEmail)
                .purpose(OtpPurpose.RESET_PASSWORD)
                .build();

        LocalDateTime requested20SecondsAgo = LocalDateTime.now().minusSeconds(20);
        EmailOtpToken recentToken = EmailOtpToken.builder()
                .id(1L)
                .email(sampleEmail)
                .otpCode("123456")
                .purpose(OtpPurpose.RESET_PASSWORD)
                .expiresAt(LocalDateTime.now().plusMinutes(4))
                .attempts(0)
                .verified(false)
                .lastRequestedAt(requested20SecondsAgo)
                .build();

        when(userRepository.findByEmail(sampleEmail)).thenReturn(Optional.of(sampleUser));
        when(otpTokenRepository.findTopByEmailAndPurposeOrderByIdDesc(sampleEmail, OtpPurpose.RESET_PASSWORD))
                .thenReturn(Optional.of(recentToken));

        OtpActionResponse response = otpService.sendOtp(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertNotNull(response.getCooldownSeconds());
        assertTrue(response.getCooldownSeconds() > 0 && response.getCooldownSeconds() <= 41);
        assertTrue(response.getMessage().contains("Vui lòng đợi"));

        verify(otpTokenRepository, never()).save(any(EmailOtpToken.class));
        verify(emailService, never()).sendOtpEmail(anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("sendOtp_ResetPassword_UserNotFound: Yeu cau OTP de RESET_PASSWORD nhung email khong ton tai -> Tra ve success=false")
    void sendOtp_ResetPassword_UserNotFound() {
        String nonExistentEmail = "ghost@viecultures.com";
        SendOtpRequest request = SendOtpRequest.builder()
                .email(nonExistentEmail)
                .purpose(OtpPurpose.RESET_PASSWORD)
                .build();

        when(userRepository.findByEmail(nonExistentEmail)).thenReturn(Optional.empty());

        OtpActionResponse response = otpService.sendOtp(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(0, response.getCooldownSeconds());
        assertTrue(response.getMessage().contains("Không tìm thấy tài khoản nào gắn với địa chỉ email này"));

        verify(otpTokenRepository, never()).findTopByEmailAndPurposeOrderByIdDesc(anyString(), any());
        verify(otpTokenRepository, never()).save(any(EmailOtpToken.class));
        verify(emailService, never()).sendOtpEmail(anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("verifyOtp_Success: Nhap dung ma OTP con han va chua su dung -> Tra ve success=true va verified=true")
    void verifyOtp_Success() {
        String correctOtp = "654321";
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email(sampleEmail)
                .otpCode(correctOtp)
                .purpose(OtpPurpose.RESET_PASSWORD)
                .build();

        EmailOtpToken validToken = EmailOtpToken.builder()
                .id(10L)
                .email(sampleEmail)
                .otpCode(correctOtp)
                .purpose(OtpPurpose.RESET_PASSWORD)
                .expiresAt(LocalDateTime.now().plusMinutes(3))
                .attempts(1)
                .verified(false)
                .build();

        when(otpTokenRepository.findTopByEmailAndPurposeOrderByIdDesc(sampleEmail, OtpPurpose.RESET_PASSWORD))
                .thenReturn(Optional.of(validToken));

        OtpActionResponse response = otpService.verifyOtp(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.getVerified());
        assertEquals("Xác thực mã OTP thành công!", response.getMessage());

        assertTrue(validToken.getVerified());
        verify(otpTokenRepository, times(1)).save(validToken);
    }

    @Test
    @DisplayName("verifyOtp_Success_VerifyEmailPurpose: Xac thuc thanh cong voi muc dich VERIFY_EMAIL -> Cap nhat emailVerified cho User")
    void verifyOtp_Success_VerifyEmailPurpose() {
        String correctOtp = "777888";
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email(sampleEmail)
                .otpCode(correctOtp)
                .purpose(OtpPurpose.VERIFY_EMAIL)
                .build();

        EmailOtpToken validToken = EmailOtpToken.builder()
                .id(11L)
                .email(sampleEmail)
                .otpCode(correctOtp)
                .purpose(OtpPurpose.VERIFY_EMAIL)
                .expiresAt(LocalDateTime.now().plusMinutes(4))
                .attempts(0)
                .verified(false)
                .build();

        when(otpTokenRepository.findTopByEmailAndPurposeOrderByIdDesc(sampleEmail, OtpPurpose.VERIFY_EMAIL))
                .thenReturn(Optional.of(validToken));
        when(userRepository.findByEmail(sampleEmail)).thenReturn(Optional.of(sampleUser));

        OtpActionResponse response = otpService.verifyOtp(request);

        assertTrue(response.isSuccess());
        assertTrue(response.getVerified());
        assertTrue(Boolean.TRUE.equals(sampleUser.getEmailVerified()));
        verify(userRepository, times(1)).save(sampleUser);
        verify(otpTokenRepository, times(1)).save(validToken);
    }

    @Test
    @DisplayName("verifyOtp_MaxAttemptsExceeded: Nhap sai vuot qua so lan cho phep (5 lan) -> Bao loi va tu choi")
    void verifyOtp_MaxAttemptsExceeded() {
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email(sampleEmail)
                .otpCode("000000")
                .purpose(OtpPurpose.RESET_PASSWORD)
                .build();

        EmailOtpToken exceededToken = EmailOtpToken.builder()
                .id(12L)
                .email(sampleEmail)
                .otpCode("654321")
                .purpose(OtpPurpose.RESET_PASSWORD)
                .expiresAt(LocalDateTime.now().plusMinutes(2))
                .attempts(5)
                .verified(false)
                .build();

        when(otpTokenRepository.findTopByEmailAndPurposeOrderByIdDesc(sampleEmail, OtpPurpose.RESET_PASSWORD))
                .thenReturn(Optional.of(exceededToken));

        OtpActionResponse response = otpService.verifyOtp(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertFalse(response.getVerified());
        assertTrue(response.getMessage().contains("Bạn đã nhập sai mã quá 5 lần"));
        verify(otpTokenRepository, never()).save(any(EmailOtpToken.class));
    }

    @Test
    @DisplayName("verifyOtp_Expired: Ma OTP da het han thoi gian hop le -> Tra ve success=false")
    void verifyOtp_Expired() {
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email(sampleEmail)
                .otpCode("123456")
                .purpose(OtpPurpose.RESET_PASSWORD)
                .build();

        EmailOtpToken expiredToken = EmailOtpToken.builder()
                .id(13L)
                .email(sampleEmail)
                .otpCode("123456")
                .purpose(OtpPurpose.RESET_PASSWORD)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .attempts(0)
                .verified(false)
                .build();

        when(otpTokenRepository.findTopByEmailAndPurposeOrderByIdDesc(sampleEmail, OtpPurpose.RESET_PASSWORD))
                .thenReturn(Optional.of(expiredToken));

        OtpActionResponse response = otpService.verifyOtp(request);

        assertFalse(response.isSuccess());
        assertFalse(response.getVerified());
        assertTrue(response.getMessage().contains("Mã OTP đã hết hạn"));
        verify(otpTokenRepository, never()).save(any(EmailOtpToken.class));
    }

    @Test
    @DisplayName("verifyOtp_NotFound: Khong tim thay ban ghi OTP trong database -> Tra ve success=false")
    void verifyOtp_NotFound() {
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email(sampleEmail)
                .otpCode("123456")
                .purpose(OtpPurpose.RESET_PASSWORD)
                .build();

        when(otpTokenRepository.findTopByEmailAndPurposeOrderByIdDesc(sampleEmail, OtpPurpose.RESET_PASSWORD))
                .thenReturn(Optional.empty());

        OtpActionResponse response = otpService.verifyOtp(request);

        assertFalse(response.isSuccess());
        assertFalse(response.getVerified());
        assertTrue(response.getMessage().contains("Không tìm thấy yêu cầu OTP hợp lệ"));
    }

    @Test
    @DisplayName("verifyOtp_WrongCode: Nhap sai ma OTP -> Tang so lan attempts va tra ve success=false")
    void verifyOtp_WrongCode() {
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email(sampleEmail)
                .otpCode("999999")
                .purpose(OtpPurpose.RESET_PASSWORD)
                .build();

        EmailOtpToken token = EmailOtpToken.builder()
                .id(14L)
                .email(sampleEmail)
                .otpCode("123456")
                .purpose(OtpPurpose.RESET_PASSWORD)
                .expiresAt(LocalDateTime.now().plusMinutes(3))
                .attempts(1)
                .verified(false)
                .build();

        when(otpTokenRepository.findTopByEmailAndPurposeOrderByIdDesc(sampleEmail, OtpPurpose.RESET_PASSWORD))
                .thenReturn(Optional.of(token));

        OtpActionResponse response = otpService.verifyOtp(request);

        assertFalse(response.isSuccess());
        assertFalse(response.getVerified());
        assertTrue(response.getMessage().contains("Mã OTP không chính xác"));
        assertEquals(2, token.getAttempts());
        verify(otpTokenRepository, times(1)).save(token);
    }

    @Test
    @DisplayName("resetPassword_Success: Dat lai mat khau thanh cong -> Ma hoa password, luu user, xoa OTP va gui thong bao")
    void resetPassword_Success() {
        String newPassword = "NewSecurePassword123";
        String otpCode = "555666";
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email(sampleEmail)
                .otpCode(otpCode)
                .newPassword(newPassword)
                .build();

        EmailOtpToken verifiedToken = EmailOtpToken.builder()
                .id(20L)
                .email(sampleEmail)
                .otpCode(otpCode)
                .purpose(OtpPurpose.RESET_PASSWORD)
                .expiresAt(LocalDateTime.now().plusMinutes(2))
                .attempts(0)
                .verified(true)
                .build();

        when(otpTokenRepository.findTopByEmailAndPurposeOrderByIdDesc(sampleEmail, OtpPurpose.RESET_PASSWORD))
                .thenReturn(Optional.of(verifiedToken));
        when(userRepository.findByEmail(sampleEmail)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("encoded_new_secure_password");

        OtpActionResponse response = otpService.resetPassword(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("Đặt lại mật khẩu thành công"));
        assertEquals("encoded_new_secure_password", sampleUser.getPassword());

        verify(userRepository, times(1)).save(sampleUser);
        verify(otpTokenRepository, times(1)).deleteAllByEmailAndPurpose(sampleEmail, OtpPurpose.RESET_PASSWORD);
        verify(emailService, times(1)).sendPasswordChangedNotification(sampleEmail, sampleUser.getName());
    }

    @Test
    @DisplayName("resetPassword_ShortPassword: Mat khau ngan hon 6 ky tu -> Tu choi yeu cau")
    void resetPassword_ShortPassword() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email(sampleEmail)
                .otpCode("123456")
                .newPassword("12345")
                .build();

        OtpActionResponse response = otpService.resetPassword(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Mật khẩu mới phải có tối thiểu 6 ký tự"));

        verify(otpTokenRepository, never()).findTopByEmailAndPurposeOrderByIdDesc(anyString(), any());
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendPasswordChangedNotification(anyString(), anyString());
    }

    @Test
    @DisplayName("resetPassword_TokenNotVerified: Token chua duoc xac minh -> Tu choi dat lai mat khau")
    void resetPassword_TokenNotVerified() {
        String otpCode = "555666";
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email(sampleEmail)
                .otpCode(otpCode)
                .newPassword("ValidPassword123")
                .build();

        EmailOtpToken unverifiedToken = EmailOtpToken.builder()
                .id(21L)
                .email(sampleEmail)
                .otpCode(otpCode)
                .purpose(OtpPurpose.RESET_PASSWORD)
                .expiresAt(LocalDateTime.now().plusMinutes(2))
                .attempts(0)
                .verified(false)
                .build();

        when(otpTokenRepository.findTopByEmailAndPurposeOrderByIdDesc(sampleEmail, OtpPurpose.RESET_PASSWORD))
                .thenReturn(Optional.of(unverifiedToken));

        OtpActionResponse response = otpService.resetPassword(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Mã OTP chưa được xác minh hoặc không chính xác"));

        verify(userRepository, never()).save(any(User.class));
        verify(otpTokenRepository, never()).deleteAllByEmailAndPurpose(anyString(), any());
    }

    @Test
    @DisplayName("resetPassword_TokenExpired: Token da het han -> Tu choi dat lai mat khau")
    void resetPassword_TokenExpired() {
        String otpCode = "555666";
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email(sampleEmail)
                .otpCode(otpCode)
                .newPassword("ValidPassword123")
                .build();

        EmailOtpToken expiredToken = EmailOtpToken.builder()
                .id(22L)
                .email(sampleEmail)
                .otpCode(otpCode)
                .purpose(OtpPurpose.RESET_PASSWORD)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .attempts(0)
                .verified(true)
                .build();

        when(otpTokenRepository.findTopByEmailAndPurposeOrderByIdDesc(sampleEmail, OtpPurpose.RESET_PASSWORD))
                .thenReturn(Optional.of(expiredToken));

        OtpActionResponse response = otpService.resetPassword(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Phiên đổi mật khẩu đã hết hạn"));

        verify(userRepository, never()).save(any(User.class));
    }
}
