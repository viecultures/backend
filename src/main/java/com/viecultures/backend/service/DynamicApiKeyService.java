package com.viecultures.backend.service;

import com.viecultures.backend.entity.AiServiceConfig;

public interface DynamicApiKeyService {

    String getActiveKey(String provider, String serviceType);

    Long getActiveConfigId(String provider, String serviceType);

    AiServiceConfig getActiveConfig(String provider, String serviceType);

    String getActiveGeminiChatModel();

    String getActiveGeminiTranslateModel();

    String getActiveAzureRegion();

    String getActiveTtsProvider();

    void markCooldown(Long configId, int statusCode, int durationMinutes);

    void markHealthy(Long configId);

    void markError(Long configId, String errorMessage);
}
