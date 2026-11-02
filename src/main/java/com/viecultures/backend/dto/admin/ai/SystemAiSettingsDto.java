package com.viecultures.backend.dto.admin.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemAiSettingsDto {

    private String geminiChatModel;
    private String geminiTranslateModel;
    private String azureSpeechRegion;
    private String activeTtsProvider;
    private List<String> availableGeminiModels;
    private List<Map<String, String>> availableAzureRegions;
}
