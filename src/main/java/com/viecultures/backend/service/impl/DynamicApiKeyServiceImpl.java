package com.viecultures.backend.service.impl;

import com.viecultures.backend.entity.AiServiceConfig;
import com.viecultures.backend.entity.SystemAiSetting;
import com.viecultures.backend.repository.AiServiceConfigRepository;
import com.viecultures.backend.repository.SystemAiSettingRepository;
import com.viecultures.backend.service.CryptoService;
import com.viecultures.backend.service.DynamicApiKeyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class DynamicApiKeyServiceImpl implements DynamicApiKeyService {

    public static final String KEY_GEMINI_CHAT_MODEL = "gemini_chat_model";
    public static final String KEY_GEMINI_TRANSLATE_MODEL = "gemini_translate_model";
    public static final String KEY_GEMINI_TTS_MODEL = "gemini_tts_model";
    public static final String KEY_GEMINI_STT_MODEL = "gemini_stt_model";
    public static final String KEY_AZURE_SPEECH_REGION = "azure_speech_region";
    public static final String KEY_ACTIVE_TTS_PROVIDER = "active_tts_provider";

    public static final String DEFAULT_GEMINI_MODEL = "gemini-3.8-flash";
    public static final String DEFAULT_GEMINI_TTS_MODEL = "gemini-3.8-flash-tts";
    public static final String DEFAULT_GEMINI_STT_MODEL = "gemini-3.5-transcribe";
    public static final String DEFAULT_AZURE_REGION = "southeastasia";
    public static final String DEFAULT_TTS_PROVIDER = "azure";

    private final AiServiceConfigRepository configRepository;
    private final SystemAiSettingRepository systemAiSettingRepository;
    private final CryptoService cryptoService;

    @Value("${gemini.chat.api.key:${gemini.api.key:}}")
    private String defaultGeminiChatKey;

    @Value("${gemini.api.key:}")
    private String defaultGeminiApiKey;




    @Value("${azure.speech.key:}")
    private String defaultAzureSpeechKey;

    @Value("${azure.speech.region:southeastasia}")
    private String defaultAzureSpeechRegion;

    @Value("${tts.provider:azure}")
    private String defaultTtsProvider;

    public DynamicApiKeyServiceImpl(
            AiServiceConfigRepository configRepository,
            SystemAiSettingRepository systemAiSettingRepository,
            CryptoService cryptoService) {
        this.configRepository = configRepository;
        this.systemAiSettingRepository = systemAiSettingRepository;
        this.cryptoService = cryptoService;
    }

    @Override
    @Transactional
    public String getActiveKey(String provider, String serviceType) {
        Optional<AiServiceConfig> selectedConfig = resolveActiveConfig(provider, serviceType);
        if (selectedConfig.isPresent()) {
            AiServiceConfig config = selectedConfig.get();
            try {
                return cryptoService.decrypt(config.getEncryptedKey());
            } catch (Exception e) {
                log.error("Khong the giai ma API key cho configId={}: {}", config.getId(), e.getMessage());
                markError(config.getId(), "Loi giai ma khoa");
            }
        }

        return getFallbackKey(provider, serviceType);
    }

    @Override
    @Transactional
    public Long getActiveConfigId(String provider, String serviceType) {
        return resolveActiveConfig(provider, serviceType)
                .map(AiServiceConfig::getId)
                .orElse(null);
    }

    @Override
    @Transactional
    public AiServiceConfig getActiveConfig(String provider, String serviceType) {
        return resolveActiveConfig(provider, serviceType).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public String getActiveGeminiChatModel() {
        Optional<SystemAiSetting> setting = systemAiSettingRepository.findBySettingKey(KEY_GEMINI_CHAT_MODEL);
        if (setting.isPresent() && setting.get().getSettingValue() != null && !setting.get().getSettingValue().isBlank()) {
            return setting.get().getSettingValue().trim();
        }

        AiServiceConfig config = getActiveConfig("GEMINI", "CHAT");
        if (config != null && config.getModelName() != null && !config.getModelName().isBlank()) {
            return config.getModelName().trim();
        }

        return DEFAULT_GEMINI_MODEL;
    }

    @Override
    @Transactional(readOnly = true)
    public String getActiveGeminiTranslateModel() {
        Optional<SystemAiSetting> setting = systemAiSettingRepository.findBySettingKey(KEY_GEMINI_TRANSLATE_MODEL);
        if (setting.isPresent() && setting.get().getSettingValue() != null && !setting.get().getSettingValue().isBlank()) {
            return setting.get().getSettingValue().trim();
        }

        AiServiceConfig config = getActiveConfig("GEMINI", "TRANSLATE");
        if (config != null && config.getModelName() != null && !config.getModelName().isBlank()) {
            return config.getModelName().trim();
        }

        return DEFAULT_GEMINI_MODEL;
    }

    @Override
    @Transactional(readOnly = true)
    public String getActiveGeminiTtsModel() {
        Optional<SystemAiSetting> setting = systemAiSettingRepository.findBySettingKey(KEY_GEMINI_TTS_MODEL);
        if (setting.isPresent() && setting.get().getSettingValue() != null && !setting.get().getSettingValue().isBlank()) {
            return setting.get().getSettingValue().trim();
        }

        AiServiceConfig config = getActiveConfig("GEMINI", "TTS");
        if (config != null && config.getModelName() != null && !config.getModelName().isBlank()) {
            return config.getModelName().trim();
        }

        return DEFAULT_GEMINI_TTS_MODEL;
    }

    @Override
    @Transactional(readOnly = true)
    public String getActiveGeminiSttModel() {
        Optional<SystemAiSetting> setting = systemAiSettingRepository.findBySettingKey(KEY_GEMINI_STT_MODEL);
        if (setting.isPresent() && setting.get().getSettingValue() != null && !setting.get().getSettingValue().isBlank()) {
            return setting.get().getSettingValue().trim();
        }

        AiServiceConfig config = getActiveConfig("GEMINI", "STT");
        if (config != null && config.getModelName() != null && !config.getModelName().isBlank()) {
            return config.getModelName().trim();
        }

        return DEFAULT_GEMINI_STT_MODEL;
    }

    @Override
    @Transactional(readOnly = true)
    public String getActiveAzureRegion() {
        Optional<SystemAiSetting> setting = systemAiSettingRepository.findBySettingKey(KEY_AZURE_SPEECH_REGION);
        if (setting.isPresent() && setting.get().getSettingValue() != null && !setting.get().getSettingValue().isBlank()) {
            return setting.get().getSettingValue().trim();
        }

        AiServiceConfig config = getActiveConfig("AZURE", "TTS");
        if (config != null && config.getRegion() != null && !config.getRegion().isBlank()) {
            return config.getRegion().trim();
        }

        if (defaultAzureSpeechRegion != null && !defaultAzureSpeechRegion.isBlank()) {
            return defaultAzureSpeechRegion.trim();
        }

        return DEFAULT_AZURE_REGION;
    }

    @Override
    @Transactional(readOnly = true)
    public String getActiveTtsProvider() {
        Optional<SystemAiSetting> setting = systemAiSettingRepository.findBySettingKey(KEY_ACTIVE_TTS_PROVIDER);
        if (setting.isPresent() && setting.get().getSettingValue() != null && !setting.get().getSettingValue().isBlank()) {
            return setting.get().getSettingValue().trim().toLowerCase();
        }

        if (defaultTtsProvider != null && !defaultTtsProvider.isBlank()) {
            return defaultTtsProvider.trim().toLowerCase();
        }

        return DEFAULT_TTS_PROVIDER;
    }

    @Override
    @Transactional
    public void markCooldown(Long configId, int statusCode, int durationMinutes) {
        if (configId == null) {
            return;
        }
        configRepository.findById(configId).ifPresent(config -> {
            int cooldownDuration = durationMinutes > 0 ? durationMinutes : (statusCode == 429 ? 15 : 60);
            LocalDateTime cooldownUntil = LocalDateTime.now().plusMinutes(cooldownDuration);
            config.setStatus("COOLDOWN");
            config.setCooldownUntil(cooldownUntil);
            configRepository.save(config);
            log.warn("Config ID {} chuyen sang trang thai COOLDOWN den {} (HTTP {})", configId, cooldownUntil, statusCode);
        });
    }

    @Override
    @Transactional
    public void markHealthy(Long configId) {
        if (configId == null) {
            return;
        }
        configRepository.findById(configId).ifPresent(config -> {
            if (!"HEALTHY".equalsIgnoreCase(config.getStatus()) || config.getCooldownUntil() != null) {
                config.setStatus("HEALTHY");
                config.setCooldownUntil(null);
                configRepository.save(config);
                log.info("Config ID {} da duoc khoi phuc trang thai HEALTHY", configId);
            }
        });
    }

    @Override
    @Transactional
    public void markError(Long configId, String errorMessage) {
        if (configId == null) {
            return;
        }
        configRepository.findById(configId).ifPresent(config -> {
            config.setStatus("ERROR");
            configRepository.save(config);
            log.error("Config ID {} duoc danh dau trang thai ERROR: {}", configId, errorMessage);
        });
    }

    private Optional<AiServiceConfig> resolveActiveConfig(String provider, String serviceType) {
        String safeProvider = (provider != null) ? provider.trim().toUpperCase() : "GEMINI";
        String safeServiceType = (serviceType != null) ? serviceType.trim().toUpperCase() : "CHAT";

        List<AiServiceConfig> configs = configRepository
                .findByProviderAndServiceTypeAndActiveTrueOrderByPriorityAsc(safeProvider, safeServiceType);

        LocalDateTime now = LocalDateTime.now();

        for (AiServiceConfig config : configs) {
            if ("COOLDOWN".equalsIgnoreCase(config.getStatus())) {
                if (config.getCooldownUntil() != null && now.isAfter(config.getCooldownUntil())) {
                    config.setStatus("HEALTHY");
                    config.setCooldownUntil(null);
                    configRepository.save(config);
                    return Optional.of(config);
                }
                continue;
            }

            if ("ERROR".equalsIgnoreCase(config.getStatus())) {
                continue;
            }

            return Optional.of(config);
        }

        List<AiServiceConfig> providerConfigs = configRepository
                .findByProviderAndActiveTrueOrderByPriorityAsc(safeProvider);

        for (AiServiceConfig config : providerConfigs) {
            if ("COOLDOWN".equalsIgnoreCase(config.getStatus())) {
                if (config.getCooldownUntil() != null && now.isAfter(config.getCooldownUntil())) {
                    config.setStatus("HEALTHY");
                    config.setCooldownUntil(null);
                    configRepository.save(config);
                    return Optional.of(config);
                }
                continue;
            }

            if ("ERROR".equalsIgnoreCase(config.getStatus())) {
                continue;
            }

            return Optional.of(config);
        }

        return Optional.empty();
    }

    private String getFallbackKey(String provider, String serviceType) {
        String safeProvider = (provider != null) ? provider.trim().toUpperCase() : "GEMINI";
        String safeServiceType = (serviceType != null) ? serviceType.trim().toUpperCase() : "CHAT";

        if ("AZURE".equals(safeProvider)) {
            return defaultAzureSpeechKey;
        }

        if ("GEMINI".equals(safeProvider)) {
            if ("TTS".equalsIgnoreCase(safeServiceType)) {
                return (defaultGeminiApiKey != null && !defaultGeminiApiKey.trim().isEmpty())
                        ? defaultGeminiApiKey : defaultGeminiChatKey;
            }
            return (defaultGeminiChatKey != null && !defaultGeminiChatKey.trim().isEmpty())
                    ? defaultGeminiChatKey : defaultGeminiApiKey;
        }

        return defaultGeminiApiKey;
    }
}
