package com.viecultures.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.viecultures.backend.dto.CompanionChatMessage;
import com.viecultures.backend.dto.CompanionChatRequest;
import com.viecultures.backend.dto.CompanionChatResponse;
import com.viecultures.backend.dto.ReferencedLessonDto;
import com.viecultures.backend.entity.ParagraphPair;
import com.viecultures.backend.entity.ReadingPost;
import com.viecultures.backend.repository.ReadingPostRepository;
import com.viecultures.backend.service.CompanionChatService;
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
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.chat.api.key}")
    private String geminiApiKey;

    @Value("${gemini.chat.api.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiApiUrl;

    // Primary and failover models for text chat
    private static final List<String> GEMINI_CHAT_MODELS = List.of(
            "gemini-3.8-flash",
            "gemini-3.5-flash",
            "gemini-2.5-flash",
            "gemini-flash-latest"
    );

    private static final int FREE_TIER_DAILY_LIMIT = 3;
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
                .reply(parsed.reply)
                .suggestedPrompts(parsed.prompts)
                .referencedLessons(referencedLessons)
                .remainingDailyTurns(remainingDailyTurns)
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
                    "LIMIT_EXCEEDED: You have used all 3 daily free companion chat turns. Please upgrade to Pro for unlimited access."
            );
        }

        int updatedCount = currentCount.incrementAndGet();
        return Math.max(0, FREE_TIER_DAILY_LIMIT - updatedCount);
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
        // Remove common punctuation to improve search matching
        return message.replaceAll("[?!.,;'\"]", " ").trim();
    }

    private String generateCompanionReply(CompanionChatRequest request, String groundedContext) {
        String systemInstruction = buildSystemPrompt(groundedContext);

        for (String model : GEMINI_CHAT_MODELS) {
            try {
                String endpoint = String.format("%s/%s:generateContent?key=%s", geminiApiUrl, model, geminiApiKey);

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

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode respNode = objectMapper.readTree(response.getBody());
                    JsonNode candidates = respNode.path("candidates");
                    if (candidates.isArray() && !candidates.isEmpty()) {
                        JsonNode parts = candidates.get(0).path("content").path("parts");
                        if (parts.isArray() && !parts.isEmpty()) {
                            return parts.get(0).path("text").asText("");
                        }
                    }
                }
            } catch (HttpStatusCodeException httpEx) {
                log.warn("Gemini model [{}] failed with HTTP {}: {}", model, httpEx.getStatusCode(), httpEx.getResponseBodyAsString());
                continue; // Try failover model
            } catch (Exception ex) {
                log.error("Unexpected error invoking Gemini model [{}]: {}", model, ex.getMessage(), ex);
                continue; // Try failover model
            }
        }

        // Graceful fallback when all Gemini endpoints fail
        log.warn("All Gemini chat models failed. Triggering offline fallback response.");
        return "Hey there! I am having a brief connection hiccup retrieving cultural archives right now. While I reconnect, what traditional dish or custom would you love to explore together?\n"
                + "PROMPTS:\n"
                + "- Tell me what makes Hanoi Pho so special.\n"
                + "- What are the most meaningful customs of Vietnamese Tet?";
    }

    private String buildSystemPrompt(String groundedContext) {
        return "You are VieCultures Cultural Companion, an authentic, warm, and engaging conversational partner passionate about Vietnamese culture, heritage, folklore, traditions, and cuisine.\n\n"
                + "Your mission: Converse with users in natural, fluid, and encouraging English (accessible for CEFR A2-B2 learners). Speak like a knowledgeable local friend sharing a story over coffee, not an encyclopedia or formal assistant.\n\n"
                + "[CULTURAL CONTEXT ARCHIVE]\n"
                + groundedContext + "\n\n"
                + "[NATURAL CONVERSATION PRINCIPLES]\n"
                + "1. Strictly English: Always respond in natural, friendly English.\n"
                + "2. Be human and warm: Never use robotic AI filler or canned templates (e.g. 'I am delighted you are asking...', 'As an AI...', 'According to the database...', 'While our library is expanding...'). Jump right into the conversation with genuine warmth and enthusiasm.\n"
                + "3. Avoid rigid formatting: Do not format responses with labeled blocks (e.g. '[Summary]:', '[Cultural Context]:'), raw asterisk dividers like '***', or heavy bullet lists unless specifically requested. Write smooth, vivid, well-paced paragraphs.\n"
                + "4. Natural storytelling: Seamlessly integrate cultural knowledge from [CULTURAL CONTEXT ARCHIVE] into your dialogue. If the topic is broader, share accurate, captivating cultural insights with curiosity.\n"
                + "5. Length: Keep replies concise and easy to read (around 80-150 words).\n"
                + "6. Conversation prompts: End your response with exactly 2 thoughtful follow-up questions to keep the chat going, formatted as:\n"
                + "PROMPTS:\n"
                + "- <Question 1>\n"
                + "- <Question 2>";
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

        // Remove standalone divider lines like ***, ---, ___ on their own line
        reply = reply.replaceAll("(?m)^\\s*[*\\-_]{3,}\\s*$", "");

        // Clean any residual trailing markdown headers, dividers, or asterisks (such as ***, ---, ###)
        reply = reply.replaceAll("(?s)[\\s*\\-_#]+$", "").trim();

        // Collapse multiple blank lines
        reply = reply.replaceAll("\\n{3,}", "\n\n").trim();

        if (prompts.isEmpty()) {
            prompts.add("What other traditions are connected to this?");
            prompts.add("Can you tell me about the regional flavors?");
        }

        return new ParsedCompanionOutput(reply, prompts);
    }

    private record ParsedCompanionOutput(String reply, List<String> prompts) {}
}
