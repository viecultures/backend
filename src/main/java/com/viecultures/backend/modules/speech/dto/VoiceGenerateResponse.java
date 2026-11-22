package com.viecultures.backend.modules.speech.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceGenerateResponse {
    private boolean success;
    private String audioUrl;
    private String filename;
    private String voiceName;
    private String mimeType;
    private long fileSizeBytes;
    private String message;
    private LocalDateTime timestamp;
}
