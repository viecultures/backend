package com.viecultures.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * GeminiVoiceService — Strictly generates voices from Google Gemini API.
 * No local TTS fallback.
 */
@Slf4j
@Service
public class GeminiVoiceService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DynamicApiKeyService dynamicApiKeyService;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model}")
    private String geminiModel;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${audio.output.dir}")
    private String audioOutputDir;

    // Supported Gemini TTS models for failover across Gemini API quotas
    private static final List<String> GEMINI_MODELS = List.of(
            "gemini-3.8-flash-tts",
            "gemini-2.5-flash-preview-tts",
            "gemini-3.1-flash-tts-preview",
            "gemini-3.8-flash");

    public GeminiVoiceService(RestTemplate restTemplate, ObjectMapper objectMapper, DynamicApiKeyService dynamicApiKeyService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.dynamicApiKeyService = dynamicApiKeyService;
    }

    public String getGeminiApiKey() {
        if (dynamicApiKeyService != null) {
            String dbKey = dynamicApiKeyService.getActiveKey("GEMINI", "LIVE");
            if (dbKey != null && !dbKey.isBlank()) {
                return dbKey.trim();
            }
            dbKey = dynamicApiKeyService.getActiveKey("GEMINI", "CHAT");
            if (dbKey != null && !dbKey.isBlank()) {
                return dbKey.trim();
            }
            dbKey = dynamicApiKeyService.getActiveKey("GEMINI", "TTS");
            if (dbKey != null && !dbKey.isBlank()) {
                return dbKey.trim();
            }
        }
        return (geminiApiKey != null && !geminiApiKey.isBlank()) ? geminiApiKey.trim() : null;
    }

    /**
     * Hàm sinh file voice cho bài viết mới trực tiếp từ Google Gemini API
     * 
     * @param textContent    Nội dung bài học tiếng Anh
     * @param voiceName      Tên giọng Gemini ('Puck', 'Charon', 'Kore', 'Fenrir',
     *                       'Aoede')
     * @param outputFilename Tên file lưu trên server (ví dụ: story_female.wav)
     * @return Đường dẫn audio tương đối truy cập được từ web
     *         (/audio/{outputFilename})
     */
    public String generatePostAudio(String textContent, String voiceName, String outputFilename) {
        if (textContent == null || textContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung textContent không được để trống");
        }
        if (voiceName == null || voiceName.trim().isEmpty()) {
            voiceName = "Puck";
        }
        if (outputFilename == null || outputFilename.trim().isEmpty()) {
            outputFilename = "gemini_" + System.currentTimeMillis() + "_" + voiceName.toLowerCase() + ".wav";
        }
        if (!outputFilename.endsWith(".wav")) {
            outputFilename = outputFilename + ".wav";
        }

        // Try primary model first, followed by other Gemini models if quota exceeded
        List<String> modelsToTry = new ArrayList<>();
        modelsToTry.add(geminiModel);
        for (String m : GEMINI_MODELS) {
            if (!modelsToTry.contains(m)) {
                modelsToTry.add(m);
            }
        }

        Exception lastException = null;
        for (String model : modelsToTry) {
            try {
                log.info("Generating voice via Google Gemini API: model={}, voice={}, file={}", model, voiceName,
                        outputFilename);
                return generateWithGemini(textContent, voiceName, outputFilename, model);
            } catch (HttpStatusCodeException httpEx) {
                lastException = httpEx;
                log.warn("Gemini model [{}] returned HTTP {}: {}", model, httpEx.getStatusCode(),
                        httpEx.getResponseBodyAsString());
                // If 429 quota or 404 model not found, try next Gemini model
                if (httpEx.getStatusCode().value() == 429 || httpEx.getStatusCode().value() == 404) {
                    continue;
                }
                break;
            } catch (Exception e) {
                lastException = e;
                log.error("Gemini model [{}] error: {}", model, e.getMessage());
            }
        }

        throw new RuntimeException("Không thể sinh voice từ Google Gemini API: " +
                (lastException != null ? lastException.getMessage() : "Unknown error"), lastException);
    }

    /**
     * Sinh dữ liệu âm thanh byte[] trực tiếp từ Gemini API cho TTS Service
     */
    public byte[] synthesizeAudioBytes(String textContent, String voiceName) throws Exception {
        if (textContent == null || textContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung textContent không được để trống");
        }
        String resolvedVoice = mapVoiceToGemini(voiceName);

        List<String> modelsToTry = new ArrayList<>();
        modelsToTry.add(geminiModel);
        for (String m : GEMINI_MODELS) {
            if (!modelsToTry.contains(m)) {
                modelsToTry.add(m);
            }
        }

        Exception lastException = null;
        for (String model : modelsToTry) {
            try {
                log.info("Synthesizing audio bytes via Google Gemini API: model={}, voice={}", model, resolvedVoice);
                return callGeminiAudioApi(textContent, resolvedVoice, model);
            } catch (HttpStatusCodeException httpEx) {
                lastException = httpEx;
                log.warn("Gemini model [{}] returned HTTP {}: {}", model, httpEx.getStatusCode(),
                        httpEx.getResponseBodyAsString());
                if (httpEx.getStatusCode().value() == 429 || httpEx.getStatusCode().value() == 404) {
                    continue;
                }
                break;
            } catch (Exception e) {
                lastException = e;
                log.error("Gemini model [{}] error: {}", model, e.getMessage());
            }
        }

        throw new RuntimeException("Không thể sinh voice từ Google Gemini API: " +
                (lastException != null ? lastException.getMessage() : "Unknown error"), lastException);
    }

    public String mapVoiceToGemini(String voiceName) {
        if (voiceName == null || voiceName.trim().isEmpty()) {
            return "Puck";
        }
        String v = voiceName.trim();
        if (List.of("Puck", "Charon", "Kore", "Fenrir", "Aoede").contains(v)) {
            return v;
        }
        String lower = v.toLowerCase();
        if (lower.contains("amanda") || lower.contains("jenny") || lower.contains("aria") || lower.contains("female")) {
            return "Aoede";
        }
        if (lower.contains("dustin") || lower.contains("guy") || lower.contains("davis") || lower.contains("male")) {
            return "Puck";
        }
        return "Puck";
    }

    /**
     * Primary generator: Google Gemini API with Audio Modality
     */
    private String generateWithGemini(String textContent, String voiceName, String outputFilename, String model)
            throws Exception {
        byte[] audioBytes = callGeminiAudioApi(textContent, voiceName, model);
        saveAudioFile(audioBytes, outputFilename);
        log.info("Successfully generated Gemini voice! File: {}, Size: {} bytes", outputFilename, audioBytes.length);
        return "/audio/" + outputFilename;
    }

    private byte[] callGeminiAudioApi(String textContent, String voiceName, String model) throws Exception {
        String prompt = "You are a professional English teacher. Read the following text verbatim, clearly and naturally with proper pauses:\n\n"
                + textContent;

        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> contentItem = Map.of(
                "role", "user",
                "parts", List.of(textPart));

        Map<String, Object> prebuiltVoiceConfig = Map.of("voiceName", voiceName);
        Map<String, Object> voiceConfig = Map.of("prebuiltVoiceConfig", prebuiltVoiceConfig);
        Map<String, Object> speechConfig = Map.of("voiceConfig", voiceConfig);

        Map<String, Object> generationConfig = Map.of(
                "responseModalities", List.of("AUDIO"),
                "speechConfig", speechConfig);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(contentItem),
                "generationConfig", generationConfig);

        String effectiveApiKey = getGeminiApiKey();
        if (effectiveApiKey == null || effectiveApiKey.isBlank()) {
            throw new IllegalStateException("Không tìm thấy Google Gemini API Key trong hệ thống (database hoặc cấu hình).");
        }
        String endpoint = String.format("%s/%s:generateContent?key=%s", geminiApiUrl, model, effectiveApiKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(endpoint, entity, String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode candidate = root.path("candidates").get(0);
        if (candidate == null) {
            throw new RuntimeException("No candidates returned by Gemini: " + response.getBody());
        }

        JsonNode parts = candidate.path("content").path("parts");
        JsonNode audioPart = null;
        for (JsonNode part : parts) {
            JsonNode inlineData = part.path("inlineData");
            if (!inlineData.isMissingNode() && inlineData.path("mimeType").asText("").startsWith("audio/")) {
                audioPart = part;
                break;
            }
        }

        if (audioPart == null) {
            throw new RuntimeException("No audio part found in Gemini response: " + response.getBody());
        }

        String mimeType = audioPart.path("inlineData").path("mimeType").asText();
        String base64Data = audioPart.path("inlineData").path("data").asText();
        byte[] audioBytes = Base64.getDecoder().decode(base64Data);

        // If raw PCM without container header, wrap in 24kHz 16-bit Mono WAV container
        if (mimeType.contains("pcm") || (!hasAudioHeader(audioBytes))) {
            audioBytes = wrapPcmInWav(audioBytes, 24000, 1, 16);
        }

        return audioBytes;
    }

    private boolean hasAudioHeader(byte[] data) {
        if (data.length < 4)
            return false;
        // Check RIFF (WAV)
        if (data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F')
            return true;
        // Check ID3 (MP3)
        if (data[0] == 'I' && data[1] == 'D' && data[2] == '3')
            return true;
        // Check MPEG frame sync 0xFF 0xFB/0xF3/0xF2
        if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xE0) == 0xE0)
            return true;
        // Check OggS
        if (data[0] == 'O' && data[1] == 'g' && data[2] == 'g' && data[3] == 'S')
            return true;
        return false;
    }

    private byte[] wrapPcmInWav(byte[] pcmData, int sampleRate, int channels, int bitsPerSample) {
        int byteRate = sampleRate * channels * (bitsPerSample / 8);
        int blockAlign = channels * (bitsPerSample / 8);
        int totalDataLen = pcmData.length;
        int totalAudioLen = totalDataLen + 36;

        byte[] header = new byte[44];
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalAudioLen & 0xff);
        header[5] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[6] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[7] = (byte) ((totalAudioLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) blockAlign;
        header[33] = 0;
        header[34] = (byte) bitsPerSample;
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalDataLen & 0xff);
        header[41] = (byte) ((totalDataLen >> 8) & 0xff);
        header[42] = (byte) ((totalDataLen >> 16) & 0xff);
        header[43] = (byte) ((totalDataLen >> 24) & 0xff);

        byte[] wav = new byte[header.length + pcmData.length];
        System.arraycopy(header, 0, wav, 0, header.length);
        System.arraycopy(pcmData, 0, wav, header.length, pcmData.length);
        return wav;
    }

    private Path resolveOutputDir() throws Exception {
        Path path = Paths.get(audioOutputDir);
        if (!path.isAbsolute()) {
            path = Paths.get("").toAbsolutePath().resolve(path).normalize();
        }
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        return path;
    }

    private void saveAudioFile(byte[] audioBytes, String outputFilename) throws Exception {
        Path dir = resolveOutputDir();
        Path targetFile = dir.resolve(outputFilename);
        if (targetFile.getParent() != null && !Files.exists(targetFile.getParent())) {
            Files.createDirectories(targetFile.getParent());
        }
        try (FileOutputStream fos = new FileOutputStream(targetFile.toFile())) {
            fos.write(audioBytes);
        }
    }
}
