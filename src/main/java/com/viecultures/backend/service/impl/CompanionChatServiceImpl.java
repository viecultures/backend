package com.viecultures.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.viecultures.backend.dto.CompanionChatMessage;
import com.viecultures.backend.dto.CompanionChatRequest;
import com.viecultures.backend.dto.CompanionChatResponse;
import com.viecultures.backend.dto.CompanionTranslateRequest;
import com.viecultures.backend.dto.CompanionTranslateResponse;
import com.viecultures.backend.dto.ReferencedLessonDto;
import com.viecultures.backend.entity.CompanionChatMessageEntity;
import com.viecultures.backend.entity.ParagraphPair;
import com.viecultures.backend.entity.ReadingPost;
import com.viecultures.backend.repository.CompanionChatMessageRepository;
import com.viecultures.backend.repository.ReadingPostRepository;
import com.viecultures.backend.service.AiCostTrackingService;
import com.viecultures.backend.service.CompanionChatService;
import com.viecultures.backend.service.DynamicApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanionChatServiceImpl implements CompanionChatService {

    private final ReadingPostRepository readingPostRepository;
    private final CompanionChatMessageRepository companionChatMessageRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DynamicApiKeyService dynamicApiKeyService;
    private final AiCostTrackingService aiCostTrackingService;

    @Value("${gemini.chat.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.chat.api.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiApiUrl;

    // Default and failover models for Gemini text generation
    private static final List<String> DEFAULT_GEMINI_FALLBACK_MODELS = List.of(
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-1.5-pro",
            "gemini-flash-latest"
    );

    private static final int FREE_TIER_DAILY_LIMIT = 5;
    private final ConcurrentHashMap<String, AtomicInteger> dailyUsageMap = new ConcurrentHashMap<>();

    @Override
    @Transactional(readOnly = true)
    public CompanionChatResponse chat(CompanionChatRequest request, String clientIdentifier, boolean isPro) {
        String effectiveClient = (clientIdentifier != null && !clientIdentifier.isBlank())
                ? clientIdentifier.trim()
                : "anonymous_guest";

        // 1. Quota Enforcement
        int remainingDailyTurns = enforceQuotaAndCalculateRemaining(effectiveClient, isPro);

        // 2. Context Retrieval & Grounding
        List<ReferencedLessonDto> referencedLessons = new ArrayList<>();
        String groundedContext = retrieveContext(request, referencedLessons);

        // 3. Gemini Generation with In-Context Injection
        String rawGeminiReply = generateCompanionReply(request, groundedContext);

        // 4. Parse response & suggested prompts
        ParsedCompanionOutput parsed = parseResponseAndPrompts(rawGeminiReply);

        return CompanionChatResponse.builder()
                .reply(parsed.reply())
                .suggestedPrompts(parsed.prompts())
                .referencedLessons(referencedLessons)
                .remainingDailyTurns(remainingDailyTurns)
                .build();
    }

    @Override
    public CompanionTranslateResponse translate(CompanionTranslateRequest request) {
        if (request == null || request.getText() == null || request.getText().isBlank()) {
            return CompanionTranslateResponse.builder().translatedText("").build();
        }

        String source = (request.getSourceLang() != null && !request.getSourceLang().isBlank())
                ? request.getSourceLang().trim().toLowerCase()
                : "auto";
        String target = (request.getTargetLang() != null && !request.getTargetLang().isBlank())
                ? request.getTargetLang().trim().toLowerCase()
                : "en";

        String systemInstruction = buildTranslationSystemPrompt(source, target);
        String translated = invokeGeminiTranslation(request.getText(), systemInstruction);

        return CompanionTranslateResponse.builder()
                .translatedText(translated)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanionChatMessage> getChatHistory(String clientIdentifier, String lessonId) {
        if (clientIdentifier == null || clientIdentifier.isBlank()) {
            return Collections.emptyList();
        }

        List<CompanionChatMessageEntity> entities;
        if (lessonId != null && !lessonId.isBlank()) {
            entities = companionChatMessageRepository.findByClientIdentifierAndLessonIdOrderByCreatedAtAsc(
                    clientIdentifier.trim(), lessonId.trim()
            );
        } else {
            entities = companionChatMessageRepository.findByClientIdentifierOrderByCreatedAtAsc(
                    clientIdentifier.trim()
            );
        }

        return entities.stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public List<CompanionChatMessage> saveChatHistory(String clientIdentifier, String lessonId, List<CompanionChatMessage> messages) {
        if (clientIdentifier == null || clientIdentifier.isBlank() || messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }

        String effectiveLessonId = (lessonId != null && !lessonId.isBlank()) ? lessonId.trim() : null;
        List<CompanionChatMessageEntity> entitiesToSave = new ArrayList<>();

        for (CompanionChatMessage msg : messages) {
            if (msg.getContent() == null || msg.getContent().isBlank()) {
                continue;
            }
            String msgLessonId = (effectiveLessonId != null) ? effectiveLessonId : msg.getLessonId();
            CompanionChatMessageEntity entity = CompanionChatMessageEntity.builder()
                    .clientIdentifier(clientIdentifier.trim())
                    .lessonId(msgLessonId)
                    .role(msg.getRole() != null ? msg.getRole() : "user")
                    .content(msg.getContent())
                    .translatedContent(msg.getTranslatedContent())
                    .build();
            entitiesToSave.add(entity);
        }

        if (entitiesToSave.isEmpty()) {
            return Collections.emptyList();
        }

        // Replace previous conversation state to prevent duplicates upon reload
        if (effectiveLessonId != null) {
            companionChatMessageRepository.deleteByClientIdentifierAndLessonId(clientIdentifier.trim(), effectiveLessonId);
        } else {
            companionChatMessageRepository.deleteByClientIdentifier(clientIdentifier.trim());
        }

        List<CompanionChatMessageEntity> saved = companionChatMessageRepository.saveAll(entitiesToSave);
        return saved.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public void clearChatHistory(String clientIdentifier, String lessonId) {
        if (clientIdentifier == null || clientIdentifier.isBlank()) {
            return;
        }

        if (lessonId != null && !lessonId.isBlank()) {
            companionChatMessageRepository.deleteByClientIdentifierAndLessonId(
                    clientIdentifier.trim(), lessonId.trim()
            );
            log.info("Cleared companion chat history for client: {} and lessonId: {}", clientIdentifier, lessonId);
        } else {
            companionChatMessageRepository.deleteByClientIdentifier(clientIdentifier.trim());
            log.info("Cleared all companion chat history for client: {}", clientIdentifier);
        }
    }

    private CompanionChatMessage toDto(CompanionChatMessageEntity entity) {
        return CompanionChatMessage.builder()
                .id(entity.getId())
                .role(entity.getRole())
                .content(entity.getContent())
                .translatedContent(entity.getTranslatedContent())
                .lessonId(entity.getLessonId())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private int enforceQuotaAndCalculateRemaining(String clientIdentifier, boolean isPro) {
        if (isPro) {
            return -1; // -1 represents unlimited tier
        }

        String usageKey = clientIdentifier + "_" + LocalDate.now();
        AtomicInteger currentCount = dailyUsageMap.computeIfAbsent(usageKey, k -> new AtomicInteger(0));

        if (currentCount.get() >= FREE_TIER_DAILY_LIMIT) {
            log.warn("Rate limit exceeded for client: {}. Daily limit: {}", clientIdentifier, FREE_TIER_DAILY_LIMIT);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "LIMIT_EXCEEDED: You have used all 5 daily free companion chat turns. Please upgrade to Pro for unlimited access."
            );
        }

        int updatedCount = currentCount.incrementAndGet();
        return Math.max(0, FREE_TIER_DAILY_LIMIT - updatedCount);
    }

    @Override
    public int getRemainingTurns(String clientIdentifier, boolean isPro) {
        if (isPro) {
            return -1;
        }
        String usageKey = clientIdentifier + "_" + LocalDate.now();
        AtomicInteger currentCount = dailyUsageMap.get(usageKey);
        int usedCount = (currentCount != null) ? currentCount.get() : 0;
        return Math.max(0, FREE_TIER_DAILY_LIMIT - usedCount);
    }

    private String retrieveContext(CompanionChatRequest request, List<ReferencedLessonDto> referencedLessons) {
        StringBuilder contextBuilder = new StringBuilder();

        // Mode A: Lesson-scoped grounding
        if (request.getLessonId() != null && !request.getLessonId().isBlank()) {
            Optional<ReadingPost> postOpt = readingPostRepository.findById(request.getLessonId().trim());
            if (postOpt.isPresent()) {
                ReadingPost post = postOpt.get();
                referencedLessons.add(toReferencedDto(post));
                appendPostToContext(contextBuilder, post);
                return contextBuilder.toString();
            }
        }

        // Mode B: Global search across repository
        String searchTarget = extractSearchKeywords(request.getMessage());
        if (!searchTarget.isBlank()) {
            List<ReadingPost> matched = readingPostRepository.searchPosts(searchTarget, PageRequest.of(0, 3));
            if (!matched.isEmpty()) {
                for (ReadingPost post : matched) {
                    referencedLessons.add(toReferencedDto(post));
                    appendPostToContext(contextBuilder, post);
                }
                return contextBuilder.toString();
            }
        }

        // Mode C: Fallback (No direct article match found)
        return "No specific article matched in the library. Share authentic, vivid cultural insights naturally from general knowledge.";
    }

    private void appendPostToContext(StringBuilder sb, ReadingPost post) {
        sb.append("--- ARTICLE CONTEXT ---\n")
                .append("Title EN: ").append(post.getTitleEn()).append("\n")
                .append("Title VI: ").append(post.getTitleVi()).append("\n")
                .append("Topic: ").append(post.getTopic()).append(", Region: ").append(post.getRegion()).append("\n")
                .append("Summary: ").append(post.getSummary() != null ? post.getSummary() : "").append("\n");

        if (post.getParagraphs() != null && !post.getParagraphs().isEmpty()) {
            sb.append("Key Sections:\n");
            int count = 0;
            for (ParagraphPair pair : post.getParagraphs()) {
                if (count++ >= 3) break;
                sb.append("- EN: ").append(pair.getEnText()).append("\n")
                        .append("  VI: ").append(pair.getViText()).append("\n");
            }
        }
        sb.append("\n");
    }

    private ReferencedLessonDto toReferencedDto(ReadingPost post) {
        return ReferencedLessonDto.builder()
                .id(post.getId())
                .titleEn(post.getTitleEn())
                .titleVi(post.getTitleVi())
                .topic(post.getTopic())
                .region(post.getRegion())
                .build();
    }

    private String extractSearchKeywords(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        return message.replaceAll("[?!.,;'\"]", " ").trim();
    }

    private String generateCompanionReply(CompanionChatRequest request, String groundedContext) {
        String systemInstruction = buildSystemPrompt(groundedContext, request.getLanguage());

        Long configId = dynamicApiKeyService.getActiveConfigId("GEMINI", "CHAT");
        String activeKey = dynamicApiKeyService.getActiveKey("GEMINI", "CHAT");
        String keyToUse = (activeKey != null && !activeKey.isBlank()) ? activeKey : geminiApiKey;

        String primaryModel = dynamicApiKeyService.getActiveGeminiChatModel();
        List<String> modelsToTry = resolveCandidateModels(primaryModel);

        for (String model : modelsToTry) {
            long startTime = System.currentTimeMillis();
            try {
                String endpoint = String.format("%s/%s:generateContent?key=%s", geminiApiUrl, model, keyToUse);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                ObjectNode rootNode = objectMapper.createObjectNode();

                // System Instruction
                ObjectNode sysNode = rootNode.putObject("systemInstruction");
                ArrayNode sysParts = sysNode.putArray("parts");
                sysParts.addObject().put("text", systemInstruction);

                // Contents (History + Current user prompt)
                ArrayNode contentsArray = rootNode.putArray("contents");

                if (request.getHistory() != null) {
                    for (CompanionChatMessage msg : request.getHistory()) {
                        if (msg.getContent() != null && !msg.getContent().isBlank()) {
                            ObjectNode histNode = contentsArray.addObject();
                            String role = "assistant".equalsIgnoreCase(msg.getRole()) ? "model" : "user";
                            histNode.put("role", role);
                            ArrayNode parts = histNode.putArray("parts");
                            parts.addObject().put("text", msg.getContent());
                        }
                    }
                }

                // Current message
                ObjectNode userTurn = contentsArray.addObject();
                userTurn.put("role", "user");
                ArrayNode userParts = userTurn.putArray("parts");
                userParts.addObject().put("text", request.getMessage());

                // Generation Config - Optimizing for Gemini 3.8
                ObjectNode genConfig = rootNode.putObject("generationConfig");
                genConfig.put("temperature", 0.7);
                genConfig.put("maxOutputTokens", 2048);
                ObjectNode thinkingConfig = genConfig.putObject("thinkingConfig");
                thinkingConfig.put("thinkingBudget", 0);

                HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(rootNode), headers);
                ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, String.class);

                int latencyMs = (int) (System.currentTimeMillis() - startTime);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode respNode = objectMapper.readTree(response.getBody());
                    JsonNode usageMetadata = respNode.path("usageMetadata");
                    int promptTokens = usageMetadata.path("promptTokenCount").asInt(0);
                    int completionTokens = usageMetadata.path("candidatesTokenCount").asInt(0);
                    aiCostTrackingService.recordUsage(configId, "GEMINI", "COMPANION_CHAT", model, promptTokens, completionTokens, 0, latencyMs, 200);

                    JsonNode candidates = respNode.path("candidates");
                    if (candidates.isArray() && !candidates.isEmpty()) {
                        JsonNode parts = candidates.get(0).path("content").path("parts");
                        if (parts.isArray() && !parts.isEmpty()) {
                            return parts.get(0).path("text").asText("");
                        }
                    }
                }
            } catch (HttpStatusCodeException httpEx) {
                int latencyMs = (int) (System.currentTimeMillis() - startTime);
                log.warn("Gemini model [{}] failed with HTTP {}: {}", model, httpEx.getStatusCode(), httpEx.getResponseBodyAsString());
                if (httpEx.getStatusCode().value() == 429 || httpEx.getStatusCode().value() == 403) {
                    dynamicApiKeyService.markCooldown(configId, httpEx.getStatusCode().value(), 15);
                }
                aiCostTrackingService.recordUsage(configId, "GEMINI", "COMPANION_CHAT", model, 0, 0, 0, latencyMs, httpEx.getStatusCode().value());
            } catch (Exception ex) {
                int latencyMs = (int) (System.currentTimeMillis() - startTime);
                log.error("Unexpected error invoking Gemini model [{}]: {}", model, ex.getMessage(), ex);
                aiCostTrackingService.recordUsage(configId, "GEMINI", "COMPANION_CHAT", model, 0, 0, 0, latencyMs, 500);
            }
        }

        // Graceful fallback when all Gemini endpoints fail
        log.warn("All Gemini chat models failed. Triggering offline fallback response.");
        String lang = (request.getLanguage() != null) ? request.getLanguage().trim().toLowerCase() : "en";
        if ("vi".equals(lang)) {
            return "Chao ban! Toi dang gap chut gian doan khi ket noi voi kho luu tru van hoa luc nay. Trong luc cho doi, ban muon cung toi tim hieu ve mon an hay phong tuc truyen thong nao cua Viet Nam?\n"
                    + "PROMPTS:\n"
                    + "- Dieu gi lam nen huong vi dac biet cua Pho Ha Noi?\n"
                    + "- Nhung phong tuc y nghia nhat trong dip Tet co truyen la gi?";
        } else if ("bilingual".equals(lang)) {
            return "Hey there! I am having a brief connection hiccup retrieving cultural archives right now. While I reconnect, what traditional dish or custom would you love to explore together?\n\n"
                    + "(Chao ban! Toi dang gap chut gian doan khi ket noi voi kho luu tru van hoa luc nay. Ban muon cung toi tim hieu ve mon an hay phong tuc truyen thong nao nhe?)\n"
                    + "PROMPTS:\n"
                    + "- What makes Hanoi Pho so special? (Dieu gi lam nen huong vi Pho Ha Noi?)\n"
                    + "- What are the most meaningful customs of Tet? (Nhung phong tuc y nghia nhat ngay Tet?)";
        } else {
            return "Hey there! I am having a brief connection hiccup retrieving cultural archives right now. While I reconnect, what traditional dish or custom would you love to explore together?\n"
                    + "PROMPTS:\n"
                    + "- Tell me what makes Hanoi Pho so special.\n"
                    + "- What are the most meaningful customs of Vietnamese Tet?";
        }
    }

    private String resolveLanguageInstruction(String language) {
        String lang = (language != null) ? language.trim().toLowerCase() : "en";
        switch (lang) {
            case "vi":
                return "[LANGUAGE REQUIREMENT]\n"
                        + "1. Strictly Vietnamese: You MUST reply entirely in natural, warm, and authentic Vietnamese (Tieng Viet).\n"
                        + "2. Do not respond in English unless quoting specific proper nouns or English terminology.\n"
                        + "3. The 2 follow-up questions in PROMPTS must be written in Vietnamese.\n\n";
            case "bilingual":
                return "[LANGUAGE REQUIREMENT]\n"
                        + "1. Bilingual Format: You MUST provide a rich bilingual response.\n"
                        + "2. First deliver the explanation in natural, warm English (accessible for CEFR A2-B2 learners).\n"
                        + "3. Next provide an authentic Vietnamese translation and cultural explanation (Tieng Viet) immediately following.\n"
                        + "4. The 2 follow-up questions in PROMPTS must include both English and Vietnamese translations.\n\n";
            case "en":
            default:
                return "[LANGUAGE REQUIREMENT]\n"
                        + "1. Strictly English: Always respond in natural, friendly English (accessible for CEFR A2-B2 learners).\n"
                        + "2. The 2 follow-up questions in PROMPTS must be written in English.\n\n";
        }
    }

    private String buildSystemPrompt(String groundedContext, String language) {
        return "You are VieCultures Cultural Companion, an authentic, warm, and engaging conversational partner passionate about Vietnamese culture, heritage, folklore, traditions, and cuisine.\n\n"
                + "Your mission: Converse with users in natural, fluid, and encouraging tone. Speak like a knowledgeable local friend sharing a story over coffee, not an encyclopedia or formal assistant.\n\n"
                + resolveLanguageInstruction(language)
                + "[CULTURAL CONTEXT ARCHIVE]\n"
                + groundedContext + "\n\n"
                + "[NATURAL CONVERSATION PRINCIPLES]\n"
                + "1. Be human and warm: Never use robotic AI filler or canned templates (e.g. 'I am delighted you are asking...', 'As an AI...', 'According to the database...', 'While our library is expanding...'). Jump right into the conversation with genuine warmth and enthusiasm.\n"
                + "2. Avoid rigid formatting: Do not format responses with labeled blocks (e.g. '[Summary]:', '[Cultural Context]:'), raw asterisk dividers like '***', or heavy bullet lists unless specifically requested. Write smooth, vivid, well-paced paragraphs.\n"
                + "3. Natural storytelling: Seamlessly integrate cultural knowledge from [CULTURAL CONTEXT ARCHIVE] into your dialogue. If the topic is broader, share accurate, captivating cultural insights with curiosity.\n"
                + "4. Length: Keep replies concise and easy to read (around 80-150 words).\n"
                + "5. Conversation prompts: End your response with exactly 2 thoughtful follow-up questions to keep the chat going, formatted as:\n"
                + "PROMPTS:\n"
                + "- <Question 1>\n"
                + "- <Question 2>";
    }

    private String buildTranslationSystemPrompt(String sourceLang, String targetLang) {
        return "You are an expert Vietnamese-English cultural translator for the VieCultures platform.\n"
                + "Translate the user's text accurately between Vietnamese and English, paying close attention to cultural context, idioms, culinary vocabulary, and historical nuance.\n\n"
                + "Guidelines:\n"
                + "1. Source language: " + sourceLang + ", Target language: " + targetLang + ".\n"
                + "2. If translating Vietnamese to English: produce natural, elegant, and vivid English. Preserve iconic cultural terms (like 'ao dai', 'banh chung', 'pho', 'tet') when culturally appropriate, accompanied by subtle context.\n"
                + "3. If translating English to Vietnamese: produce fluid, idiomatic, and graceful Vietnamese, avoiding awkward literal word-for-word translation.\n"
                + "4. Output ONLY the translated text. Do NOT include greetings, preamble, explanations, markdown fences, quotation marks, or meta commentary.";
    }

    private String invokeGeminiTranslation(String text, String systemInstruction) {
        Long configId = dynamicApiKeyService.getActiveConfigId("GEMINI", "TRANSLATE");
        String activeKey = dynamicApiKeyService.getActiveKey("GEMINI", "TRANSLATE");
        String keyToUse = (activeKey != null && !activeKey.isBlank()) ? activeKey : geminiApiKey;

        String primaryModel = dynamicApiKeyService.getActiveGeminiTranslateModel();
        List<String> modelsToTry = resolveCandidateModels(primaryModel);

        for (String model : modelsToTry) {
            long startTime = System.currentTimeMillis();
            try {
                String endpoint = String.format("%s/%s:generateContent?key=%s", geminiApiUrl, model, keyToUse);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                ObjectNode rootNode = objectMapper.createObjectNode();

                // System Instruction
                ObjectNode sysNode = rootNode.putObject("systemInstruction");
                ArrayNode sysParts = sysNode.putArray("parts");
                sysParts.addObject().put("text", systemInstruction);

                // Contents
                ArrayNode contentsArray = rootNode.putArray("contents");
                ObjectNode userTurn = contentsArray.addObject();
                userTurn.put("role", "user");
                ArrayNode userParts = userTurn.putArray("parts");
                userParts.addObject().put("text", text);

                // Generation Config
                ObjectNode genConfig = rootNode.putObject("generationConfig");
                genConfig.put("temperature", 0.3);
                genConfig.put("maxOutputTokens", 2048);
                ObjectNode thinkingConfig = genConfig.putObject("thinkingConfig");
                thinkingConfig.put("thinkingBudget", 0);

                HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(rootNode), headers);
                ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, String.class);

                int latencyMs = (int) (System.currentTimeMillis() - startTime);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode respNode = objectMapper.readTree(response.getBody());
                    JsonNode usageMetadata = respNode.path("usageMetadata");
                    int promptTokens = usageMetadata.path("promptTokenCount").asInt(0);
                    int completionTokens = usageMetadata.path("candidatesTokenCount").asInt(0);
                    aiCostTrackingService.recordUsage(configId, "GEMINI", "COMPANION_TRANSLATE", model, promptTokens, completionTokens, 0, latencyMs, 200);

                    JsonNode candidates = respNode.path("candidates");
                    if (candidates.isArray() && !candidates.isEmpty()) {
                        JsonNode parts = candidates.get(0).path("content").path("parts");
                        if (parts.isArray() && !parts.isEmpty()) {
                            String translated = parts.get(0).path("text").asText("").trim();
                            if (!translated.isBlank()) {
                                return translated;
                            }
                        }
                    }
                }
            } catch (HttpStatusCodeException httpEx) {
                int latencyMs = (int) (System.currentTimeMillis() - startTime);
                log.warn("Gemini translation model [{}] failed with HTTP {}: {}", model, httpEx.getStatusCode(), httpEx.getResponseBodyAsString());
                if (httpEx.getStatusCode().value() == 429 || httpEx.getStatusCode().value() == 403) {
                    dynamicApiKeyService.markCooldown(configId, httpEx.getStatusCode().value(), 15);
                }
                aiCostTrackingService.recordUsage(configId, "GEMINI", "COMPANION_TRANSLATE", model, 0, 0, 0, latencyMs, httpEx.getStatusCode().value());
            } catch (Exception ex) {
                int latencyMs = (int) (System.currentTimeMillis() - startTime);
                log.error("Unexpected error in Gemini translation model [{}]: {}", model, ex.getMessage(), ex);
                aiCostTrackingService.recordUsage(configId, "GEMINI", "COMPANION_TRANSLATE", model, 0, 0, 0, latencyMs, 500);
            }
        }

        log.warn("All Gemini models failed for translation. Returning original text as fallback.");
        return text;
    }

    private List<String> resolveCandidateModels(String primaryModel) {
        List<String> models = new ArrayList<>();
        if (primaryModel != null && !primaryModel.isBlank()) {
            models.add(primaryModel.trim());
        }
        for (String fallback : DEFAULT_GEMINI_FALLBACK_MODELS) {
            if (!models.contains(fallback)) {
                models.add(fallback);
            }
        }
        return models;
    }

    private static final Pattern PROMPTS_DELIMITER_PATTERN = Pattern.compile(
            "(?i)(?:\\r?\\n|^)\\s*(?:[*\\-_]{3,}\\s*\\r?\\n\\s*)?(?:---|\\*\\*\\*|\\*\\*|###)?\\s*(?:PROMPTS?|SUGGESTED\\s+PROMPTS?|FOLLOW[- ]UP(?:\\s+QUESTIONS?)?)\\s*(?:---|\\*\\*\\*|\\*\\*|:)?\\s*(?:\\r?\\n|$)"
    );

    private ParsedCompanionOutput parseResponseAndPrompts(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ParsedCompanionOutput(
                    "Hello! I am your VieCultures Companion. What Vietnamese tradition, dish, or cultural story would you like to explore today?",
                    List.of("What makes traditional Pho broth authentic?", "Tell me about Mid-Autumn festival traditions.")
            );
        }

        String reply = raw;
        List<String> prompts = new ArrayList<>();

        Matcher matcher = PROMPTS_DELIMITER_PATTERN.matcher(raw);
        if (matcher.find()) {
            reply = raw.substring(0, matcher.start()).trim();
            String promptsBlock = raw.substring(matcher.end()).trim();
            for (String line : promptsBlock.split("\\r?\\n")) {
                String cleaned = line.replaceAll("^[\\-*•0-9.)\\]\\s]+", "").replaceAll("[*#_]+$", "").trim();
                if (!cleaned.isBlank()) {
                    prompts.add(cleaned);
                }
            }
        } else {
            int idx = raw.toUpperCase().lastIndexOf("PROMPTS:");
            if (idx != -1) {
                reply = raw.substring(0, idx).trim();
                String promptsBlock = raw.substring(idx + "PROMPTS:".length()).trim();
                for (String line : promptsBlock.split("\\r?\\n")) {
                    String cleaned = line.replaceAll("^[\\-*•0-9.)\\]\\s]+", "").replaceAll("[*#_]+$", "").trim();
                    if (!cleaned.isBlank()) {
                        prompts.add(cleaned);
                    }
                }
            }
        }

        reply = reply.replaceAll("(?m)^\\s*[*\\-_]{3,}\\s*$", "");
        reply = reply.replaceAll("(?s)[\\s*\\-_#]+$", "").trim();
        reply = reply.replaceAll("\\n{3,}", "\n\n").trim();

        if (prompts.isEmpty()) {
            prompts.add("What other traditions are connected to this?");
            prompts.add("Can you tell me about the regional flavors?");
        }

        return new ParsedCompanionOutput(reply, prompts);
    }

    private record ParsedCompanionOutput(String reply, List<String> prompts) {}
}
