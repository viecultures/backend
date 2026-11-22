package com.viecultures.backend.modules.auth.dto;

import com.viecultures.backend.entity.OtpPurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu gửi mã xác thực OTP qua Email")
public class SendOtpRequest {

    @Schema(description = "Địa chỉ email nhận mã", example = "student@viecultures.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "Mục đích gửi OTP: RESET_PASSWORD | VERIFY_EMAIL", example = "RESET_PASSWORD")
    private OtpPurpose purpose;
}
