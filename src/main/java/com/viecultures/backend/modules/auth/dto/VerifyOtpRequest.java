package com.viecultures.backend.modules.auth.dto;

import com.viecultures.backend.entity.OtpPurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu kiểm tra mã xác thực OTP")
public class VerifyOtpRequest {

    @Schema(description = "Địa chỉ email", example = "student@viecultures.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "Mã OTP 6 chữ số", example = "849201", requiredMode = Schema.RequiredMode.REQUIRED)
    private String otpCode;

    @Schema(description = "Mục đích gửi OTP: RESET_PASSWORD | VERIFY_EMAIL", example = "RESET_PASSWORD")
    private OtpPurpose purpose;
}
