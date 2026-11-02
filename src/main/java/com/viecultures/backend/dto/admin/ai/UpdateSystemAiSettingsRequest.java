package com.viecultures.backend.dto.admin.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSystemAiSettingsRequest {

    private String geminiChatModel;
    private String geminiTranslateModel;
    private String azureSpeechRegion;
    private String activeTtsProvider;
}
