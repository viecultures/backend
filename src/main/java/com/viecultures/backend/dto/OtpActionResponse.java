package com.viecultures.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả phản hồi của thao tác OTP")
public class OtpActionResponse {

    @Schema(description = "Thao tác thành công hay không", example = "true")
    private boolean success;

    @Schema(description = "Thông báo kết quả cho người dùng", example = "Mã OTP đã được gửi về email của bạn")
    private String message;

    @Schema(description = "Số giây cần chờ trước khi có thể yêu cầu gửi lại OTP", example = "60")
    private Integer cooldownSeconds;

    @Schema(description = "Trạng thái đã xác thực thành công hay chưa (dùng cho bước verify)", example = "true")
    private Boolean verified;
}
