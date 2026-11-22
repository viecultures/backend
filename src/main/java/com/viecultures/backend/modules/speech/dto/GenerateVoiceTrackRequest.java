package com.viecultures.backend.modules.speech.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu tạo voice track (nhanh & chậm) cho bài đọc")
public class GenerateVoiceTrackRequest {

    @Schema(description = "Toàn văn nội dung tiếng Anh của bài đọc", requiredMode = Schema.RequiredMode.REQUIRED)
    private String text;

    @Schema(description = "Mã model giọng đọc Azure (Mặc định: en-US-AmandaMultilingualNeural)", defaultValue = "en-US-AmandaMultilingualNeural")
    private String voiceId;

    @Schema(description = "Tên hiển thị của giọng đọc", defaultValue = "Amanda")
    private String voiceName;

    @Schema(description = "Tiêu đề hoặc slug để đặt tên file dễ nhớ", example = "keo-vo-hmong")
    private String titleSlug;
}
