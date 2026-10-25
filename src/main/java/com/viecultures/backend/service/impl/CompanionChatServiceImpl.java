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

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanionChatServiceImpl implements CompanionChatService {

    private final ReadingPostRepository readingPostRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models}")
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
        return "No specific lesson matched in the repository. Provide a general cultural explanation and gracefully suggest related cultural themes.";
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

                // Generation Config
                ObjectNode genConfig = rootNode.putObject("generationConfig");
                genConfig.put("temperature", 0.7);
                genConfig.put("maxOutputTokens", 600);

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
                if (httpEx.getStatusCode().value() == 429 || httpEx.getStatusCode().value() == 404) {
                    continue; // Try failover model
                }
            } catch (Exception ex) {
                log.error("Unexpected error invoking Gemini model [{}]: {}", model, ex.getMessage(), ex);
            }
        }

        // Graceful fallback when all Gemini endpoints fail
        log.warn("All Gemini chat models failed. Triggering offline fallback response.");
        return "I am delighted you are asking about Vietnamese cultural traditions! "
                + "Our real-time cultural companion is currently experiencing high demand. "
                + "PROMPTS:\n- What traditional dish or heritage site would you like to explore next?\n- How do you celebrate traditional festivals in your hometown?";
    }

    private String buildSystemPrompt(String groundedContext) {
        return "You are VieCultures Cultural Learning Companion, a warm, culturally knowledgeable, and encouraging language partner.\n"
                + "Your mission is to help learners explore Vietnamese culture (traditions, food, heritage) while practicing English comfortably.\n\n"
                + "[CONTEXT DATA FROM DATABASE]\n"
                + groundedContext + "\n\n"
                + "[CONSTRAINTS]\n"
                + "1. Answer in natural, friendly English suitable for CEFR A2-B2 learners. If the user asks in Vietnamese, provide a bilingual response or clear natural English explanation.\n"
                + "2. Ground facts in [CONTEXT DATA FROM DATABASE]. If the topic is not covered in the database, provide a concise cultural overview, gracefully acknowledge our library is expanding, and keep the tone encouraging.\n"
                + "3. Keep responses under 150 words.\n"
                + "4. At the very end of your response, ALWAYS output exactly 2 suggested follow-up prompts formatted as:\n"
                + "PROMPTS:\n"
                + "- <Question 1>\n"
                + "- <Question 2>";
    }

    private ParsedCompanionOutput parseResponseAndPrompts(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ParsedCompanionOutput(
                    "Hello! I am your VieCultures Companion. How can I help you explore Vietnamese culture today?",
                    List.of("Tell me about Pho Hanoi.", "What is unique about Hoi An Ancient Town?")
            );
        }

        String reply = raw;
        List<String> prompts = new ArrayList<>();

        int promptsIdx = raw.indexOf("PROMPTS:");
        if (promptsIdx != -1) {
            reply = raw.substring(0, promptsIdx).trim();
            String promptsBlock = raw.substring(promptsIdx + "PROMPTS:".length()).trim();
            for (String line : promptsBlock.split("\n")) {
                String cleaned = line.replaceAll("^[-*0-9.]\\s*", "").trim();
                if (!cleaned.isBlank()) {
                    prompts.add(cleaned);
                }
            }
        }

        if (prompts.isEmpty()) {
            prompts.add("Have you tried this tradition before?");
            prompts.add("What would you like to learn about next?");
        }

        return new ParsedCompanionOutput(reply, prompts);
    }

    private record ParsedCompanionOutput(String reply, List<String> prompts) {}
}
