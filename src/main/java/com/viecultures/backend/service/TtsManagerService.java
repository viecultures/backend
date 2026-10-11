package com.viecultures.backend.service;

import com.viecultures.backend.dto.TtsRequest;
import com.viecultures.backend.dto.TtsResult;
import com.viecultures.backend.exception.TtsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TtsManagerService {

    private final AzureSpeechService azureSpeechService;
    private final GeminiVoiceService geminiVoiceService;
    private final TtsCacheService ttsCacheService;

    @Value("${tts.provider:azure}")
    private String configuredProvider;

    @Value("${tts.fallback.enabled:true}")
    private boolean fallbackEnabled;

    /**
     * Quản lý điều phối TTS: kiểm tra bộ đệm MD5, chọn nhà cung cấp theo cấu hình (.env),
     * và tự động chuyển đổi sang dịch vụ dự phòng khi gặp sự cố.
     */
    public TtsResult synthesize(TtsRequest request) {
        String text = request.getText().trim();
        String voice = (request.getVoice() != null && !request.getVoice().trim().isEmpty())
                ? request.getVoice().trim()
                : "en-US-JennyNeural";
        String speed = (request.getSpeed() != null && !request.getSpeed().trim().isEmpty())
                ? request.getSpeed().trim()
                : "0%";

        // Ưu tiên provider truyền trong request (nếu có), nếu không dùng cấu hình TTS_PROVIDER trong .env
        String provider = (request.getProvider() != null && !request.getProvider().trim().isEmpty())
                ? request.getProvider().trim().toLowerCase()
                : configuredProvider.trim().toLowerCase();

        // 1. Tạo MD5 Hash từ (text + voice + speed)
        String hash = ttsCacheService.computeMd5(text, voice, speed);

        // 2. Kiểm tra bộ đệm File System Caching
        Optional<byte[]> cachedData = ttsCacheService.getFromCache(provider, hash);
        if (cachedData.isPresent()) {
            log.info("Audio retrieved from local cache: hash={}, provider={}", hash, provider);
            return TtsResult.builder()
                    .audioData(cachedData.get())
                    .provider(provider)
                    .fromCache(true)
                    .contentType("audio/mpeg")
                    .build();
        }

        log.info("Audio cache miss, generating via provider [{}]: hash={}", provider, hash);

        // 3. Thực thi sinh âm thanh theo Provider đã chọn
        if ("gemini".equals(provider)) {
            return generateWithGeminiProvider(text, voice, speed, hash);
        } else {
            // Mặc định là Azure Speech
            return generateWithAzureProvider(text, voice, speed, hash);
        }
    }

    private TtsResult generateWithAzureProvider(String text, String voice, String speed, String hash) {
        try {
            byte[] audioBytes = azureSpeechService.synthesize(text, voice, speed);
            ttsCacheService.putCache("azure", hash, audioBytes, ".mp3");

            return TtsResult.builder()
                    .audioData(audioBytes)
                    .provider("azure")
                    .fromCache(false)
                    .contentType("audio/mpeg")
                    .build();

        } catch (Exception ex) {
            log.warn("Azure Speech generation failed: {}.", ex.getMessage());

            if (fallbackEnabled) {
                log.info("Initiating automatic fallback to Gemini Neural Voice backup...");
                try {
                    byte[] geminiAudio = geminiVoiceService.synthesizeAudioBytes(text, voice);
                    ttsCacheService.putCache("gemini", hash, geminiAudio, ".mp3");

                    return TtsResult.builder()
                            .audioData(geminiAudio)
                            .provider("gemini-backup")
                            .fromCache(false)
                            .contentType("audio/mpeg")
                            .build();
                } catch (Exception geminiEx) {
                    log.error("Gemini TTS backup also failed: {}", geminiEx.getMessage());
                }
            }

            if (ex instanceof TtsException) {
                throw (TtsException) ex;
            }
            throw new TtsException("AZURE_GENERATION_FAILED", 500,
                    "Không thể tạo âm thanh từ Azure Speech: " + ex.getMessage());
        }
    }

    private TtsResult generateWithGeminiProvider(String text, String voice, String speed, String hash) {
        try {
            byte[] audioBytes = geminiVoiceService.synthesizeAudioBytes(text, voice);
            ttsCacheService.putCache("gemini", hash, audioBytes, ".mp3");

            return TtsResult.builder()
                    .audioData(audioBytes)
                    .provider("gemini")
                    .fromCache(false)
                    .contentType("audio/mpeg")
                    .build();

        } catch (Exception ex) {
            log.warn("Gemini TTS generation failed: {}.", ex.getMessage());

            if (fallbackEnabled && azureSpeechService.isConfigured()) {
                log.info("Initiating automatic fallback to Azure Speech backup...");
                try {
                    byte[] azureAudio = azureSpeechService.synthesize(text, voice, speed);
                    ttsCacheService.putCache("azure", hash, azureAudio, ".mp3");

                    return TtsResult.builder()
                            .audioData(azureAudio)
                            .provider("azure-backup")
                            .fromCache(false)
                            .contentType("audio/mpeg")
                            .build();
                } catch (Exception azureEx) {
                    log.error("Azure Speech backup also failed: {}", azureEx.getMessage());
                }
            }

            throw new TtsException("GEMINI_GENERATION_FAILED", 500,
                    "Không thể tạo âm thanh từ Google Gemini AI: " + ex.getMessage());
        }
    }
}
