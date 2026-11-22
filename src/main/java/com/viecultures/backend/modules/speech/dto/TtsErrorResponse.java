package com.viecultures.backend.modules.speech.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin phản hồi khi xảy ra lỗi TTS")
public class TtsErrorResponse {

    @Schema(description = "Trạng thái thành công", example = "false")
    private boolean success;

    @Schema(description = "Mã định danh lỗi", example = "AZURE_UNAUTHORIZED")
    private String error;

    @Schema(description = "Mô tả chi tiết nguyên nhân lỗi", example = "Azure Speech API Key không hợp lệ hoặc đã hết hạn (HTTP 401)")
    private String message;

    @Schema(description = "Mã trạng thái HTTP tương ứng", example = "401")
    private int status;

    @Schema(description = "Thời gian xảy ra lỗi")
    private LocalDateTime timestamp;
}
