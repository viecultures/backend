package com.viecultures.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.viecultures.backend.dto.SttCorrectionDetail;
import com.viecultures.backend.dto.TwoPassSttResponse;
import com.viecultures.backend.entity.AiServiceConfig;
import com.viecultures.backend.service.AiCostTrackingService;
import com.viecultures.backend.service.DynamicApiKeyService;
import com.viecultures.backend.service.TwoPassSttService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Triển khai dịch vụ Two-Pass Speech-to-Text:
 * - Pass 1: Nhận dạng âm học thô (Acoustic Transcription) qua Gemini Multimodal (mô hình chính: gemini-3.5-transcribe).
 * - Pass 2: Tinh chỉnh ngữ nghĩa (Semantic Refinement & Cleanup) loại bỏ filler words, sửa false starts khi smartCleanup = true.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TwoPassSttServiceImpl implements TwoPassSttService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DynamicApiKeyService dynamicApiKeyService;

    @Autowired(required = false)
    private AiCostTrackingService aiCostTrackingService;

    @Value("${gemini.api.key:}")
    private String defaultGeminiApiKey;

    @Value("${gemini.chat.api.key:}")
    private String defaultGeminiChatKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiApiUrl;

    private static final List<String> DEFAULT_PASS1_MODELS = List.of(
            "gemini-3.5-transcribe",
            "gemini-2.0-flash",
            "gemini-1.5-flash"
    );

    private static final List<String> PASS2_CANDIDATE_MODELS = List.of(
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-flash-latest"
    );

    private static final String PASS1_PROMPT =
            "Transcribe the spoken audio verbatim. "
                    + "STRICT RULE: This platform strictly supports ONLY English and Vietnamese. "
                    + "Output ONLY the exact spoken English words (or Vietnamese if spoken). "
                    + "Do NOT translate, do NOT output or guess words in any other foreign languages (e.g., Chinese, Japanese, Korean, Spanish, French). "
                    + "Do NOT omit filler words, stutters, repetitions, or grammatical errors; transcribe every single utterance verbatim. "
                    + "If the audio is silent or unintelligible noise, respond with EMPTY_SPEECH.";

    private static final String PASS2_SYSTEM_PROMPT =
            "You are an expert English dialogue editor for language learners.\n"
                    + "STRICT LANGUAGE RULE: Only English and Vietnamese are permitted. Do NOT output any third languages.\n"
                    + "Input: verbatim raw transcript of an English learner.\n"
                    + "Tasks:\n"
                    + "1. Remove verbal fillers and hesitation (um, uh, er, ah, like, you know).\n"
                    + "2. Repair self-corrections/false starts (e.g. 'She go... I mean she goes' -> 'She goes').\n"
                    + "3. Fix context homophones, capitalization, and punctuation.\n"
                    + "4. Keep the learner's vocabulary level intact.\n"
                    + "Return strictly a valid JSON object:\n"
                    + "{\"cleaned_text\": \"...\", \"corrections\": [{\"original\": \"...\", \"replacement\": \"...\", \"type\": \"...\", \"explanation\": \"...\"}]}";

    private static final Pattern MARKDOWN_JSON_PATTERN = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```");

    @Override
    public TwoPassSttResponse transcribe(MultipartFile file) {
        return transcribe(file, true);
    }

    @Override
    public TwoPassSttResponse transcribe(MultipartFile file, boolean smartCleanup) {
        long overallStartTime = System.currentTimeMillis();

        if (file == null || file.isEmpty()) {
            log.warn("Tập tin âm thanh tải lên rỗng hoặc null.");
            return TwoPassSttResponse.builder()
                    .rawText("")
                    .cleanedText("")
                    .corrections(Collections.emptyList())
                    .confidence(0.0)
                    .totalLatencyMs(0)
                    .pass1LatencyMs(0)
                    .pass2LatencyMs(0)
                    .build();
        }

        byte[] audioBytes;
        try {
            audioBytes = file.getBytes();
        } catch (Exception e) {
            log.error("Không thể đọc mảng byte từ tệp âm thanh: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Không thể đọc tệp âm thanh tải lên", e);
        }

        String mimeType = resolveMimeType(file);
        String base64Audio = Base64.getEncoder().encodeToString(audioBytes);

        String apiKey = resolveApiKey();
        Long configId = dynamicApiKeyService != null ? dynamicApiKeyService.getActiveConfigId("GEMINI", "STT") : null;

        // --- PASS 1: Acoustic Transcription (Nhận dạng âm học thô verbatim) ---
        long pass1Start = System.currentTimeMillis();
        String rawText = executePass1(base64Audio, mimeType, apiKey, configId);
        long pass1Latency = System.currentTimeMillis() - pass1Start;

        String trimmedRawText = rawText != null ? rawText.trim() : "";

        // Kiểm tra trường hợp im lặng hoặc nhiễu không xác định
        if (trimmedRawText.isEmpty() || "EMPTY_SPEECH".equalsIgnoreCase(trimmedRawText)) {
            long totalLatency = System.currentTimeMillis() - overallStartTime;
            log.info("Pass 1 trả về EMPTY_SPEECH hoặc khoảng trắng rỗng. Kết thúc tiến trình.");
            return TwoPassSttResponse.builder()
                    .rawText("")
                    .cleanedText("")
                    .corrections(Collections.emptyList())
                    .confidence(0.0)
                    .totalLatencyMs(totalLatency)
                    .pass1LatencyMs(pass1Latency)
                    .pass2LatencyMs(0)
                    .build();
        }

        // Chế độ luyện phát âm (smartCleanup = false):
        // Trả về chính xác 100% từng từ thô mà học viên đã nói (verbatim), giữ nguyên lỗi sai và từ đệm
        if (!smartCleanup) {
            long totalLatency = System.currentTimeMillis() - overallStartTime;
            log.info("Chế độ verbatim (smartCleanup=false): Giữ nguyên 100% bản ghi thô phục vụ chấm điểm phát âm.");
            return TwoPassSttResponse.builder()
                    .rawText(trimmedRawText)
                    .cleanedText(trimmedRawText)
                    .corrections(Collections.emptyList())
                    .confidence(0.95)
                    .totalLatencyMs(totalLatency)
                    .pass1LatencyMs(pass1Latency)
                    .pass2LatencyMs(0)
                    .build();
        }

        // --- PASS 2: Semantic Refinement & Cleanup (Chế độ hội thoại thông minh) ---
        long pass2Start = System.currentTimeMillis();
        Pass2Result pass2Result = executePass2WithFallback(trimmedRawText, apiKey, configId);
        long pass2Latency = System.currentTimeMillis() - pass2Start;

        long totalLatency = System.currentTimeMillis() - overallStartTime;

        return TwoPassSttResponse.builder()
                .rawText(trimmedRawText)
                .cleanedText(pass2Result.cleanedText())
                .corrections(pass2Result.corrections())
                .confidence(pass2Result.confidence())
                .totalLatencyMs(totalLatency)
                .pass1LatencyMs(pass1Latency)
                .pass2LatencyMs(pass2Latency)
                .build();
    }

    private List<String> resolvePass1CandidateModels() {
        List<String> models = new ArrayList<>();
        if (dynamicApiKeyService != null) {
            AiServiceConfig config = dynamicApiKeyService.getActiveConfig("GEMINI", "STT");
            if (config != null && config.getModelName() != null && !config.getModelName().isBlank()) {
                models.add(config.getModelName().trim());
            }
        }
        for (String defaultModel : DEFAULT_PASS1_MODELS) {
            if (!models.contains(defaultModel)) {
                models.add(defaultModel);
            }
        }
        return models;
    }

    private String executePass1(String base64Audio, String mimeType, String apiKey, Long configId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Exception lastException = null;
        List<String> candidateModels = resolvePass1CandidateModels();

        for (String model : candidateModels) {
            long callStart = System.currentTimeMillis();
            try {
                String endpoint = String.format("%s/%s:generateContent?key=%s", geminiApiUrl, model, apiKey);

                ObjectNode rootNode = objectMapper.createObjectNode();
                ArrayNode contentsArray = rootNode.putArray("contents");
                ObjectNode contentObj = contentsArray.addObject();
                ArrayNode partsArray = contentObj.putArray("parts");

                // Phần 1: Chỉ thị nhận dạng nguyên văn (verbatim)
                partsArray.addObject().put("text", PASS1_PROMPT);

                // Phần 2: Âm thanh đa phương thức Base64
                ObjectNode inlineData = partsArray.addObject().putObject("inlineData");
                inlineData.put("mimeType", mimeType);
                inlineData.put("data", base64Audio);

                // Cấu hình sinh
                ObjectNode genConfig = rootNode.putObject("generationConfig");
                genConfig.put("temperature", 0.0);
                genConfig.put("maxOutputTokens", 1024);

                HttpEntity<String> requestEntity = new HttpEntity<>(objectMapper.writeValueAsString(rootNode), headers);
                ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, requestEntity, String.class);

                int latencyMs = (int) (System.currentTimeMillis() - callStart);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    if (dynamicApiKeyService != null && configId != null) {
                        dynamicApiKeyService.markHealthy(configId);
                    }

                    JsonNode respNode = objectMapper.readTree(response.getBody());

                    int promptTokens = 0;
                    int completionTokens = 0;
                    JsonNode usageMetadata = respNode.path("usageMetadata");
                    if (!usageMetadata.isMissingNode()) {
                        promptTokens = usageMetadata.path("promptTokenCount").asInt(0);
                        completionTokens = usageMetadata.path("candidatesTokenCount").asInt(0);
                    }

                    recordAiUsage(configId, "STT", model, promptTokens, completionTokens, latencyMs, 200);

                    JsonNode candidates = respNode.path("candidates");
                    if (candidates.isArray() && !candidates.isEmpty()) {
                        JsonNode parts = candidates.get(0).path("content").path("parts");
                        if (parts.isArray() && !parts.isEmpty()) {
                            return parts.get(0).path("text").asText("");
                        }
                    }
                    return "";
                }
            } catch (HttpStatusCodeException httpEx) {
                int latencyMs = (int) (System.currentTimeMillis() - callStart);
                log.warn("Pass 1 STT thất bại với model [{}], mã trạng thái {}: {}", model, httpEx.getStatusCode(), httpEx.getResponseBodyAsString());
                if (dynamicApiKeyService != null && (httpEx.getStatusCode().value() == 429 || httpEx.getStatusCode().value() == 403)) {
                    dynamicApiKeyService.markCooldown(configId, httpEx.getStatusCode().value(), 15);
                }
                recordAiUsage(configId, "STT", model, 0, 0, latencyMs, httpEx.getStatusCode().value());
                lastException = httpEx;
            } catch (Exception ex) {
                int latencyMs = (int) (System.currentTimeMillis() - callStart);
                log.error("Lỗi bất thường khi gọi Pass 1 STT với model [{}]: {}", model, ex.getMessage(), ex);
                recordAiUsage(configId, "STT", model, 0, 0, latencyMs, 500);
                lastException = ex;
            }
        }

        log.error("Tất cả các model Gemini dùng cho Pass 1 đều thất bại. Lỗi cuối cùng: {}", lastException != null ? lastException.getMessage() : "Unknown error");
        throw new RuntimeException("Quá trình nhận dạng âm thanh (Pass 1) thất bại trên toàn bộ endpoint", lastException);
    }

    private Pass2Result executePass2WithFallback(String rawTranscript, String apiKey, Long configId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        for (String model : PASS2_CANDIDATE_MODELS) {
            long callStart = System.currentTimeMillis();
            try {
                String endpoint = String.format("%s/%s:generateContent?key=%s", geminiApiUrl, model, apiKey);

                ObjectNode rootNode = objectMapper.createObjectNode();

                // Chỉ thị hệ thống
                ObjectNode sysInstruction = rootNode.putObject("systemInstruction");
                sysInstruction.putArray("parts").addObject().put("text", PASS2_SYSTEM_PROMPT);

                // Dữ liệu đầu vào người học
                ArrayNode contents = rootNode.putArray("contents");
                ObjectNode userTurn = contents.addObject();
                userTurn.put("role", "user");
                userTurn.putArray("parts").addObject().put("text", "Raw transcript: " + rawTranscript);

                // Cấu hình sinh ép kiểu JSON
                ObjectNode genConfig = rootNode.putObject("generationConfig");
                genConfig.put("temperature", 0.1);
                genConfig.put("maxOutputTokens", 2048);
                genConfig.put("responseMimeType", "application/json");

                HttpEntity<String> requestEntity = new HttpEntity<>(objectMapper.writeValueAsString(rootNode), headers);
                ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, requestEntity, String.class);

                int latencyMs = (int) (System.currentTimeMillis() - callStart);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode respNode = objectMapper.readTree(response.getBody());

                    int promptTokens = 0;
                    int completionTokens = 0;
                    JsonNode usageMetadata = respNode.path("usageMetadata");
                    if (!usageMetadata.isMissingNode()) {
                        promptTokens = usageMetadata.path("promptTokenCount").asInt(0);
                        completionTokens = usageMetadata.path("candidatesTokenCount").asInt(0);
                    }

                    recordAiUsage(configId, "STT", model, promptTokens, completionTokens, latencyMs, 200);

                    JsonNode candidates = respNode.path("candidates");
                    if (candidates.isArray() && !candidates.isEmpty()) {
                        JsonNode parts = candidates.get(0).path("content").path("parts");
                        if (parts.isArray() && !parts.isEmpty()) {
                            String rawJsonOutput = parts.get(0).path("text").asText("");
                            return parsePass2Json(rawJsonOutput, rawTranscript);
                        }
                    }
                }
            } catch (HttpStatusCodeException httpEx) {
                int latencyMs = (int) (System.currentTimeMillis() - callStart);
                log.warn("Pass 2 STT thất bại với model [{}], mã trạng thái {}: {}", model, httpEx.getStatusCode(), httpEx.getResponseBodyAsString());
                if (dynamicApiKeyService != null && (httpEx.getStatusCode().value() == 429 || httpEx.getStatusCode().value() == 403)) {
                    dynamicApiKeyService.markCooldown(configId, httpEx.getStatusCode().value(), 15);
                }
                recordAiUsage(configId, "STT", model, 0, 0, latencyMs, httpEx.getStatusCode().value());
            } catch (Exception ex) {
                int latencyMs = (int) (System.currentTimeMillis() - callStart);
                log.error("Lỗi khi xử lý Pass 2 STT với model [{}]: {}", model, ex.getMessage(), ex);
                recordAiUsage(configId, "STT", model, 0, 0, latencyMs, 500);
            }
        }

        // Fallback an toàn: Trả về văn bản thô từ Pass 1 và danh sách chỉnh sửa rỗng
        log.warn("Toàn bộ endpoint Pass 2 gặp sự cố. Áp dụng cơ chế Fallback an toàn dùng rawText làm cleanedText.");
        return new Pass2Result(rawTranscript, Collections.emptyList(), 0.85);
    }

    private Pass2Result parsePass2Json(String rawJson, String fallbackRawText) {
        if (rawJson == null || rawJson.isBlank()) {
            return new Pass2Result(fallbackRawText, Collections.emptyList(), 0.85);
        }

        String jsonCandidate = rawJson.trim();
        Matcher matcher = MARKDOWN_JSON_PATTERN.matcher(jsonCandidate);
        if (matcher.find()) {
            jsonCandidate = matcher.group(1).trim();
        }

        try {
            JsonNode root = objectMapper.readTree(jsonCandidate);

            String cleanedText = root.path("cleaned_text").asText();
            if (cleanedText.isBlank()) {
                cleanedText = root.path("cleanedText").asText();
            }
            if (cleanedText.isBlank()) {
                cleanedText = fallbackRawText;
            }

            List<SttCorrectionDetail> corrections = new ArrayList<>();
            JsonNode correctionsNode = root.path("corrections");
            if (correctionsNode.isArray()) {
                for (JsonNode item : correctionsNode) {
                    corrections.add(SttCorrectionDetail.builder()
                            .original(item.path("original").asText(""))
                            .replacement(item.path("replacement").asText(""))
                            .type(item.path("type").asText("SELF_CORRECTION"))
                            .explanation(item.path("explanation").asText(""))
                            .build());
                }
            }

            return new Pass2Result(cleanedText, corrections, 0.96);
        } catch (Exception e) {
            log.warn("Không thể giải mã JSON từ kết quả Pass 2: {}. Sử dụng fallback rawText.", e.getMessage());
            return new Pass2Result(fallbackRawText, Collections.emptyList(), 0.88);
        }
    }

    private String resolveMimeType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank() && !"application/octet-stream".equalsIgnoreCase(contentType)) {
            return contentType.trim().toLowerCase();
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String lower = originalFilename.toLowerCase();
            if (lower.endsWith(".webm")) {
                return "audio/webm";
            }
            if (lower.endsWith(".wav")) {
                return "audio/wav";
            }
            if (lower.endsWith(".mp3")) {
                return "audio/mp3";
            }
            if (lower.endsWith(".ogg")) {
                return "audio/ogg";
            }
            if (lower.endsWith(".m4a")) {
                return "audio/m4a";
            }
        }

        return "audio/wav";
    }

    private String resolveApiKey() {
        if (dynamicApiKeyService != null) {
            String key = dynamicApiKeyService.getActiveKey("GEMINI", "STT");
            if (key != null && !key.isBlank()) {
                return key.trim();
            }
            key = dynamicApiKeyService.getActiveKey("GEMINI", "CHAT");
            if (key != null && !key.isBlank()) {
                return key.trim();
            }
        }

        if (defaultGeminiApiKey != null && !defaultGeminiApiKey.isBlank()) {
            return defaultGeminiApiKey.trim();
        }

        if (defaultGeminiChatKey != null && !defaultGeminiChatKey.isBlank()) {
            return defaultGeminiChatKey.trim();
        }

        throw new IllegalStateException("Không tìm thấy Google Gemini API Key trong cấu hình hệ thống");
    }

    private void recordAiUsage(Long configId, String featureCode, String modelName, int promptTokens, int completionTokens, int latencyMs, int httpStatus) {
        if (aiCostTrackingService != null) {
            try {
                aiCostTrackingService.recordUsage(
                        configId,
                        "GEMINI",
                        featureCode,
                        modelName,
                        promptTokens,
                        completionTokens,
                        0,
                        latencyMs,
                        httpStatus
                );
            } catch (Exception ex) {
                log.debug("Không thể ghi nhận telemetry AiUsageLog: {}", ex.getMessage());
            }
        }
    }

    private record Pass2Result(String cleanedText, List<SttCorrectionDetail> corrections, double confidence) {
    }
}
