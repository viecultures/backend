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
@Schema(description = "Kết quả tạo voice track bài đọc (bao gồm link phiên bản chậm và nhanh)")
public class VoiceTrackResponse {

    @Schema(description = "Trạng thái thành công", example = "true")
    private boolean success;

    @Schema(description = "Mã model giọng đọc", example = "en-US-AmandaMultilingualNeural")
    private String voiceId;

    @Schema(description = "Tên hiển thị của giọng đọc", example = "Amanda (Nữ - Đa ngôn ngữ)")
    private String voiceName;

    @Schema(description = "URL âm thanh phiên bản nguyên bản (chuẩn 0%) - Mặc định bài đọc", example = "/audio/posts/keo-vo_amanda_normal.mp3")
    private String normalUrl;

    @Schema(description = "URL âm thanh phiên bản chậm (-15%) dành cho người mới học / Shadowing", example = "/audio/posts/keo-vo_amanda_slow.mp3")
    private String slowUrl;

    @Schema(description = "Alias URL phiên bản gốc (tương thích ngược)", example = "/audio/posts/keo-vo_amanda_normal.mp3")
    private String fastUrl;

    @Schema(description = "Thông báo kết quả")
    private String message;

    @Schema(description = "Thời gian tạo")
    private LocalDateTime createdAt;
}
