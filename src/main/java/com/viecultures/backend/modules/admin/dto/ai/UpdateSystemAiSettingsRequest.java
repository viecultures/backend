package com.viecultures.backend.modules.admin.dto.ai;

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
    private String geminiTtsModel;
    private String geminiSttModel;
    private String azureSpeechRegion;
    private String activeTtsProvider;
}
