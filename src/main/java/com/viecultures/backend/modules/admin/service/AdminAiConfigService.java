package com.viecultures.backend.modules.admin.service;

import com.viecultures.backend.modules.admin.dto.ai.*;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdminAiConfigService {

    List<AiServiceConfigDto> getAllConfigs();

    AiServiceConfigDto getConfigById(Long id);

    AiServiceConfigDto createConfig(CreateAiKeyRequest request);

    AiServiceConfigDto updateConfig(Long id, UpdateAiKeyRequest request);

    void deleteConfig(Long id);

    AiServiceConfigDto toggleActive(Long id);

    TestAiKeyResponse testKeyConnection(TestAiKeyRequest request);

    SystemAiSettingsDto getSystemSettings();

    SystemAiSettingsDto updateSystemSettings(UpdateSystemAiSettingsRequest request);

    AiCostMetricsDto getMetrics();

    Page<AiUsageLogDto> getUsageLogs(String provider, String featureCode, int page, int size);
}
