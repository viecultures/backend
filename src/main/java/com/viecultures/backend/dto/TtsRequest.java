package com.viecultures.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu chuyển văn bản thành giọng nói (TTS)")
public class TtsRequest {

    @Schema(description = "Từ vựng hoặc đoạn văn bản tiếng Anh cần đọc (bắt buộc, tối đa 1.000 ký tự)", example = "Hello, welcome to VieCultures!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String text;

    @Schema(description = "Tên giọng đọc (tùy chọn, mặc định: en-US-JennyNeural)", example = "en-US-JennyNeural", defaultValue = "en-US-JennyNeural")
    private String voice;

    @Schema(description = "Tốc độ đọc (tùy chọn, ví dụ: '-15%' cho người mới học, mặc định: '0%')", example = "-15%", defaultValue = "0%")
    private String speed;

    @Schema(description = "Tùy chọn ghi đè nhà cung cấp: 'azure' hoặc 'gemini' (mặc định lấy theo TTS_PROVIDER trong .env)", example = "azure")
    private String provider;
}
