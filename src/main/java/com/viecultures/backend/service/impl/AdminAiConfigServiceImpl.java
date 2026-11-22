package com.viecultures.backend.service.impl;

import com.viecultures.backend.dto.admin.ai.*;
import com.viecultures.backend.entity.AiServiceConfig;
import com.viecultures.backend.entity.AiUsageLog;
import com.viecultures.backend.entity.SystemAiSetting;
import com.viecultures.backend.repository.AiServiceConfigRepository;
import com.viecultures.backend.repository.AiUsageLogRepository;
import com.viecultures.backend.repository.SystemAiSettingRepository;
import com.viecultures.backend.service.AdminAiConfigService;
import com.viecultures.backend.service.CryptoService;
import com.viecultures.backend.integration.gemini.DynamicApiKeyService;
import com.viecultures.backend.integration.gemini.impl.DynamicApiKeyServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAiConfigServiceImpl implements AdminAiConfigService {

    private static final List<Map<String, String>> AVAILABLE_AZURE_REGIONS = List.of(
            Map.of("id", "southeastasia", "name", "Đông Nam Á (Singapore)"),
            Map.of("id", "eastasia", "name", "Đông Á (Hồng Kông)"),
            Map.of("id", "centralus", "name", "Miền Trung Hoa Kỳ (Iowa)"),
            Map.of("id", "eastus", "name", "Miền Đông Hoa Kỳ (Virginia)"),
            Map.of("id", "westus", "name", "Miền Tây Hoa Kỳ (California)"),
            Map.of("id", "westeurope", "name", "Tây Âu (Hà Lan)"),
            Map.of("id", "northeurope", "name", "Bắc Âu (Ireland)"),
            Map.of("id", "japaneast", "name", "Đông Nhật Bản (Tokyo)")
    );

    private final AiServiceConfigRepository configRepository;
    private final SystemAiSettingRepository systemAiSettingRepository;
    private final AiUsageLogRepository usageLogRepository;
    private final CryptoService cryptoService;
    private final DynamicApiKeyService dynamicApiKeyService;
    private final RestTemplate restTemplate;

    @Value("${azure.speech.region:southeastasia}")
    private String azureSpeechRegion;

    @Override
    @Transactional(readOnly = true)
    public List<AiServiceConfigDto> getAllConfigs() {
        return configRepository.findAllByOrderByProviderAscServiceTypeAscPriorityAsc().stream()
                .map(this::toConfigDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AiServiceConfigDto getConfigById(Long id) {
        AiServiceConfig config = findConfigOrThrow(id);
        return toConfigDto(config);
    }

    @Override
    @Transactional
    public AiServiceConfigDto createConfig(CreateAiKeyRequest request) {
        String provider = request.getProvider().trim().toUpperCase();
        String serviceType = request.getServiceType().trim().toUpperCase();
        String keyLabel = request.getKeyLabel().trim();
        String rawKey = request.getApiKey().trim();

        String encryptedKey = cryptoService.encrypt(rawKey);
        String maskedKey = cryptoService.maskKey(rawKey);

        AiServiceConfig config = AiServiceConfig.builder()
                .provider(provider)
                .serviceType(serviceType)
                .keyLabel(keyLabel)
                .encryptedKey(encryptedKey)
                .maskedKey(maskedKey)
                .modelName(request.getModelName() != null && !request.getModelName().isBlank() ? request.getModelName().trim() : null)
                .region(request.getRegion() != null && !request.getRegion().isBlank() ? request.getRegion().trim() : null)
                .extraConfig(request.getExtraConfig())
                .priority(request.getPriority() != null ? request.getPriority() : 1)
                .active(request.getActive() != null ? request.getActive() : true)
                .status("HEALTHY")
                .build();

        AiServiceConfig saved = configRepository.save(config);
        log.info("Da tao cau hinh AI moi: id={}, provider={}, serviceType={}, label={}",
                saved.getId(), provider, serviceType, keyLabel);
        return toConfigDto(saved);
    }

    @Override
    @Transactional
    public AiServiceConfigDto updateConfig(Long id, UpdateAiKeyRequest request) {
        AiServiceConfig config = findConfigOrThrow(id);

        if (request.getKeyLabel() != null && !request.getKeyLabel().trim().isEmpty()) {
            config.setKeyLabel(request.getKeyLabel().trim());
        }

        if (request.getServiceType() != null && !request.getServiceType().isBlank()) {
            config.setServiceType(request.getServiceType().trim().toUpperCase());
        }

        if (request.getApiKey() != null && !request.getApiKey().trim().isEmpty()) {
            String rawKey = request.getApiKey().trim();
            config.setEncryptedKey(cryptoService.encrypt(rawKey));
            config.setMaskedKey(cryptoService.maskKey(rawKey));
        }

        if (request.getModelName() != null) {
            config.setModelName(request.getModelName().isBlank() ? null : request.getModelName().trim());
        }

        if (request.getRegion() != null) {
            config.setRegion(request.getRegion().isBlank() ? null : request.getRegion().trim());
        }

        if (request.getExtraConfig() != null) {
            config.setExtraConfig(request.getExtraConfig());
        }

        if (request.getPriority() != null) {
            config.setPriority(request.getPriority());
        }

        if (request.getActive() != null) {
            config.setActive(request.getActive());
        }

        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            String status = request.getStatus().trim().toUpperCase();
            config.setStatus(status);
            if ("HEALTHY".equals(status)) {
                config.setCooldownUntil(null);
            }
        }

        AiServiceConfig updated = configRepository.save(config);
        log.info("Da cap nhat cau hinh AI id={}", id);
        return toConfigDto(updated);
    }

    @Override
    @Transactional
    public void deleteConfig(Long id) {
        AiServiceConfig config = findConfigOrThrow(id);
        configRepository.delete(config);
        log.info("Da xoa cau hinh AI id={}", id);
    }

    @Override
    @Transactional
    public AiServiceConfigDto toggleActive(Long id) {
        AiServiceConfig config = findConfigOrThrow(id);
        config.setActive(!config.isActive());
        AiServiceConfig updated = configRepository.save(config);
        log.info("Da thay doi trang thai active cua config id={} sang {}", id, updated.isActive());
        return toConfigDto(updated);
    }

    @Override
    public TestAiKeyResponse testKeyConnection(TestAiKeyRequest request) {
        String provider = request.getProvider();
        String serviceType = request.getServiceType();
        String apiKey = request.getApiKey();
        String modelName = request.getModelName();
        String region = request.getRegion();
        Long configId = request.getConfigId();

        if (configId != null) {
            AiServiceConfig config = findConfigOrThrow(configId);
            provider = config.getProvider();
            serviceType = config.getServiceType();
            apiKey = cryptoService.decrypt(config.getEncryptedKey());
            if (modelName == null || modelName.isBlank()) {
                modelName = config.getModelName();
            }
            if (region == null || region.isBlank()) {
                region = config.getRegion();
            }
        }

        String safeProvider = (provider != null) ? provider.trim().toUpperCase() : "GEMINI";

        if (apiKey == null || apiKey.trim().isEmpty()) {
            if ("GEMINI".equals(safeProvider)) {
                apiKey = dynamicApiKeyService.getActiveKey("GEMINI", serviceType != null ? serviceType : "CHAT");
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    apiKey = dynamicApiKeyService.getActiveKey("GEMINI", "LIVE");
                }
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    apiKey = dynamicApiKeyService.getActiveKey("GEMINI", "TTS");
                }
            } else if ("AZURE".equals(safeProvider)) {
                apiKey = dynamicApiKeyService.getActiveKey("AZURE", "TTS");
            }
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            return TestAiKeyResponse.builder()
                    .success(false)
                    .httpStatus(400)
                    .latencyMs(0)
                    .message("Chưa có API Key hoạt động nào trong hệ thống để kiểm tra mô hình.")
                    .build();
        }
        long startTime = System.currentTimeMillis();

        try {
            if ("AZURE".equals(safeProvider)) {
                return testAzureSpeechKey(apiKey, region, startTime, configId);
            } else {
                return testGeminiKey(apiKey, modelName, startTime, configId);
            }
        } catch (HttpStatusCodeException e) {
            long latency = System.currentTimeMillis() - startTime;
            int statusCode = e.getStatusCode().value();
            log.warn("Kiem tra ket noi AI that bai HTTP {}: {}", statusCode, e.getResponseBodyAsString());
            if (configId != null && (statusCode == 429 || statusCode == 403)) {
                dynamicApiKeyService.markCooldown(configId, statusCode, 15);
            } else if (configId != null && statusCode == 401) {
                dynamicApiKeyService.markError(configId, "Khoa khong hop le hoac het han (HTTP 401)");
            }
                                String detailMsg = "Lỗi từ nhà cung cấp AI: HTTP " + statusCode;
            if (statusCode == 404) {
                detailMsg = "Mô hình không tồn tại trên Google AI Studio hoặc tài khoản không có quyền truy cập (HTTP 404).";
            } else if (statusCode == 400) {
                detailMsg = "Yêu cầu không hợp lệ hoặc định dạng mô hình không đúng (HTTP 400).";
            } else if (statusCode == 401 || statusCode == 403) {
                detailMsg = "API Key không hợp lệ hoặc không có quyền truy cập mô hình này (HTTP " + statusCode + ").";
            } else if (statusCode == 429) {
                detailMsg = "Mô hình đang bị vượt hạn ngạch (Rate Limit / Quota Exceeded - HTTP 429).";
            }
            return TestAiKeyResponse.builder()
                    .success(false)
                    .latencyMs(latency)
                    .httpStatus(statusCode)
                    .message(detailMsg)
                    .build();
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("Loi ngoai le khi kiem tra ket noi AI: {}", e.getMessage(), e);
            return TestAiKeyResponse.builder()
                    .success(false)
                    .latencyMs(latency)
                    .httpStatus(500)
                    .message("Lỗi kết nối: " + e.getMessage())
                    .build();
        }
    }

    private TestAiKeyResponse testGeminiKey(String apiKey, String requestedModel, long startTime, Long configId) {
        String model = (requestedModel != null && !requestedModel.isBlank())
                ? requestedModel.trim()
                : dynamicApiKeyService.getActiveGeminiChatModel();
        if (model == null || model.isBlank()) {
            model = "gemini-2.0-flash";
        }
        String cleanModel = model.startsWith("models/") ? model.substring(7) : model;
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + cleanModel + "?key=" + apiKey.trim();

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        long latency = System.currentTimeMillis() - startTime;

        if (response.getStatusCode().is2xxSuccessful()) {
            if (configId != null) {
                dynamicApiKeyService.markHealthy(configId);
            }
            return TestAiKeyResponse.builder()
                    .success(true)
                    .latencyMs(latency)
                    .httpStatus(response.getStatusCode().value())
                    .message("Kết nối tới Google Gemini API thành công với mô hình " + cleanModel)
                    .modelUsed(cleanModel)
                    .build();
        } else {
            return TestAiKeyResponse.builder()
                    .success(false)
                    .latencyMs(latency)
                    .httpStatus(response.getStatusCode().value())
                    .message("Phản hồi không thành công từ Google Gemini")
                    .build();
        }
    }

    private TestAiKeyResponse testAzureSpeechKey(String apiKey, String requestedRegion, long startTime, Long configId) {
        String region = (requestedRegion != null && !requestedRegion.isBlank())
                ? requestedRegion.trim()
                : dynamicApiKeyService.getActiveAzureRegion();
        if (region == null || region.isBlank()) {
            region = (azureSpeechRegion != null && !azureSpeechRegion.isBlank())
                    ? azureSpeechRegion.trim() : "southeastasia";
        }

        String url = "https://" + region + ".tts.speech.microsoft.com/cognitiveservices/voices/list";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Ocp-Apim-Subscription-Key", apiKey.trim());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        long latency = System.currentTimeMillis() - startTime;

        if (response.getStatusCode().is2xxSuccessful()) {
            if (configId != null) {
                dynamicApiKeyService.markHealthy(configId);
            }
            return TestAiKeyResponse.builder()
                    .success(true)
                    .latencyMs(latency)
                    .httpStatus(response.getStatusCode().value())
                    .message("Kết nối tới Azure Speech API thành công tại vùng " + region)
                    .modelUsed("azure-speech-" + region)
                    .build();
        } else {
            return TestAiKeyResponse.builder()
                    .success(false)
                    .latencyMs(latency)
                    .httpStatus(response.getStatusCode().value())
                    .message("Phản hồi không thành công từ Azure Speech")
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SystemAiSettingsDto getSystemSettings() {
        return SystemAiSettingsDto.builder()
                .geminiChatModel(dynamicApiKeyService.getActiveGeminiChatModel())
                .geminiTranslateModel(dynamicApiKeyService.getActiveGeminiTranslateModel())
                .geminiTtsModel(dynamicApiKeyService.getActiveGeminiTtsModel())
                .geminiSttModel(dynamicApiKeyService.getActiveGeminiSttModel())
                .azureSpeechRegion(dynamicApiKeyService.getActiveAzureRegion())
                .activeTtsProvider(dynamicApiKeyService.getActiveTtsProvider())
                .availableGeminiModels(configRepository.findAll().stream()
                        .map(AiServiceConfig::getModelName)
                        .filter(m -> m != null && !m.isBlank())
                        .distinct()
                        .collect(Collectors.toList()))
                .availableAzureRegions(AVAILABLE_AZURE_REGIONS)
                .build();
    }

    @Override
    @Transactional
    public SystemAiSettingsDto updateSystemSettings(UpdateSystemAiSettingsRequest request) {
        if (request.getGeminiChatModel() != null && !request.getGeminiChatModel().isBlank()) {
            saveOrUpdateSetting(DynamicApiKeyServiceImpl.KEY_GEMINI_CHAT_MODEL,
                    request.getGeminiChatModel().trim(), "Mô hình Gemini cho tính năng trò chuyện trợ lý ảo");
        }
        if (request.getGeminiTranslateModel() != null && !request.getGeminiTranslateModel().isBlank()) {
            saveOrUpdateSetting(DynamicApiKeyServiceImpl.KEY_GEMINI_TRANSLATE_MODEL,
                    request.getGeminiTranslateModel().trim(), "Mô hình Gemini cho tính năng dịch thuật văn bản");
        }
        if (request.getGeminiTtsModel() != null && !request.getGeminiTtsModel().isBlank()) {
            saveOrUpdateSetting(DynamicApiKeyServiceImpl.KEY_GEMINI_TTS_MODEL,
                    request.getGeminiTtsModel().trim(), "Mô hình Gemini cho tính năng tạo giọng đọc (TTS / Voice)");
        }
        if (request.getGeminiSttModel() != null && !request.getGeminiSttModel().isBlank()) {
            saveOrUpdateSetting(DynamicApiKeyServiceImpl.KEY_GEMINI_STT_MODEL,
                    request.getGeminiSttModel().trim(), "Mô hình Gemini cho tính năng nhận diện giọng nói (STT)");
        }
        if (request.getAzureSpeechRegion() != null && !request.getAzureSpeechRegion().isBlank()) {
            saveOrUpdateSetting(DynamicApiKeyServiceImpl.KEY_AZURE_SPEECH_REGION,
                    request.getAzureSpeechRegion().trim(), "Vùng máy chủ Azure AI Speech");
        }
        if (request.getActiveTtsProvider() != null && !request.getActiveTtsProvider().isBlank()) {
            saveOrUpdateSetting(DynamicApiKeyServiceImpl.KEY_ACTIVE_TTS_PROVIDER,
                    request.getActiveTtsProvider().trim().toLowerCase(), "Nhà cung cấp giọng đọc TTS chính");
        }
        return getSystemSettings();
    }

    private void saveOrUpdateSetting(String key, String value, String description) {
        SystemAiSetting setting = systemAiSettingRepository.findBySettingKey(key)
                .orElse(SystemAiSetting.builder()
                        .settingKey(key)
                        .description(description)
                        .build());
        setting.setSettingValue(value);
        if (setting.getDescription() == null || setting.getDescription().isBlank()) {
            setting.setDescription(description);
        }
        systemAiSettingRepository.save(setting);
    }

    @Override
    @Transactional(readOnly = true)
    public AiCostMetricsDto getMetrics() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        BigDecimal totalCost = usageLogRepository.calculateTotalCost();
        BigDecimal todayCost = usageLogRepository.calculateTotalCostSince(startOfToday);
        BigDecimal thisMonthCost = usageLogRepository.calculateTotalCostSince(startOfMonth);

        long totalRequests = usageLogRepository.count();
        long todayRequests = usageLogRepository.countRequestsSince(startOfToday);
        long totalTokens = usageLogRepository.calculateTotalTokens();
        long totalCharacters = usageLogRepository.calculateTotalCharacters();

        List<AiCostMetricsDto.ProviderCostBreakdownDto> providerBreakdown = new ArrayList<>();
        List<Object[]> providerRows = usageLogRepository.getCostSummaryByProvider();
        if (providerRows != null) {
            for (Object[] row : providerRows) {
                String provider = row[0] != null ? row[0].toString() : "UNKNOWN";
                BigDecimal cost = parseBigDecimal(row[1]);
                long requests = parseLong(row[2]);
                long tokens = parseLong(row[3]);
                long chars = parseLong(row[4]);

                providerBreakdown.add(AiCostMetricsDto.ProviderCostBreakdownDto.builder()
                        .provider(provider)
                        .totalCostUsd(cost)
                        .requestCount(requests)
                        .totalTokens(tokens)
                        .totalCharacters(chars)
                        .build());
            }
        }

        List<AiCostMetricsDto.FeatureCostBreakdownDto> featureBreakdown = new ArrayList<>();
        List<Object[]> featureRows = usageLogRepository.getCostSummaryByFeature();
        if (featureRows != null) {
            for (Object[] row : featureRows) {
                String feature = row[0] != null ? row[0].toString() : "UNKNOWN";
                BigDecimal cost = parseBigDecimal(row[1]);
                long requests = parseLong(row[2]);

                featureBreakdown.add(AiCostMetricsDto.FeatureCostBreakdownDto.builder()
                        .featureCode(feature)
                        .totalCostUsd(cost)
                        .requestCount(requests)
                        .build());
            }
        }

        return AiCostMetricsDto.builder()
                .totalCostUsd(totalCost)
                .todayCostUsd(todayCost)
                .thisMonthCostUsd(thisMonthCost)
                .totalRequests(totalRequests)
                .todayRequests(todayRequests)
                .totalTokens(totalTokens)
                .totalCharacters(totalCharacters)
                .providerBreakdown(providerBreakdown)
                .featureBreakdown(featureBreakdown)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AiUsageLogDto> getUsageLogs(String provider, String featureCode, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));

        boolean hasProvider = provider != null && !provider.trim().isEmpty();
        boolean hasFeature = featureCode != null && !featureCode.trim().isEmpty();

        Page<AiUsageLog> logPage;
        if (hasProvider && hasFeature) {
            logPage = usageLogRepository.findByProviderAndFeatureCodeOrderByCreatedAtDesc(
                    provider.trim().toUpperCase(), featureCode.trim().toUpperCase(), pageable);
        } else if (hasProvider) {
            logPage = usageLogRepository.findByProviderOrderByCreatedAtDesc(
                    provider.trim().toUpperCase(), pageable);
        } else if (hasFeature) {
            logPage = usageLogRepository.findByFeatureCodeOrderByCreatedAtDesc(
                    featureCode.trim().toUpperCase(), pageable);
        } else {
            logPage = usageLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return logPage.map(this::toUsageLogDto);
    }

    private AiServiceConfig findConfigOrThrow(Long id) {
        return configRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay cau hinh AI voi id: " + id));
    }

    private AiServiceConfigDto toConfigDto(AiServiceConfig config) {
        return AiServiceConfigDto.builder()
                .id(config.getId())
                .provider(config.getProvider())
                .serviceType(config.getServiceType())
                .keyLabel(config.getKeyLabel())
                .maskedKey(config.getMaskedKey())
                .modelName(config.getModelName())
                .region(config.getRegion())
                .extraConfig(config.getExtraConfig())
                .active(config.isActive())
                .priority(config.getPriority())
                .status(config.getStatus())
                .cooldownUntil(config.getCooldownUntil())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private AiUsageLogDto toUsageLogDto(AiUsageLog entity) {
        return AiUsageLogDto.builder()
                .id(entity.getId())
                .configId(entity.getConfigId())
                .provider(entity.getProvider())
                .featureCode(entity.getFeatureCode())
                .modelName(entity.getModelName())
                .promptTokens(entity.getPromptTokens())
                .completionTokens(entity.getCompletionTokens())
                .characterCount(entity.getCharacterCount())
                .estimatedCostUsd(entity.getEstimatedCostUsd())
                .latencyMs(entity.getLatencyMs())
                .httpStatus(entity.getHttpStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private BigDecimal parseBigDecimal(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private long parseLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }
}
