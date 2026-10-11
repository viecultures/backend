package com.viecultures.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceGenerateRequest {
    /**
     * Nội dung bài học tiếng Anh cần đọc
     */
    private String textContent;

    /**
     * Tên giọng ('Puck', 'Charon', 'Kore', 'Fenrir', 'Aoede')
     */
    private String voiceName;

    /**
     * Tên file lưu trên server (ví dụ: new_story_male.wav)
     */
    private String outputFilename;
}
