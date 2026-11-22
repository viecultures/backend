package com.viecultures.backend.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu đặt lại mật khẩu mới bằng OTP đã xác thực")
public class ResetPasswordRequest {

    @Schema(description = "Địa chỉ email", example = "student@viecultures.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "Mã OTP đã được gửi và kiểm tra", example = "849201", requiredMode = Schema.RequiredMode.REQUIRED)
    private String otpCode;

    @Schema(description = "Mật khẩu mới (tối thiểu 6 ký tự)", example = "VieCultures@2026", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}
