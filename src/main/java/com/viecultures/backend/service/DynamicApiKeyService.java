package com.viecultures.backend.service;

public interface DynamicApiKeyService {

    String getActiveKey(String provider, String serviceType);

    Long getActiveConfigId(String provider, String serviceType);

    void markCooldown(Long configId, int statusCode, int durationMinutes);

    void markHealthy(Long configId);

    void markError(Long configId, String errorMessage);
}
