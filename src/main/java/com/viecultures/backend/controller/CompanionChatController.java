package com.viecultures.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viecultures.backend.dto.*;
import com.viecultures.backend.service.CompanionChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/companion")
@RequiredArgsConstructor
@Tag(name = "AI Companion", description = "Endpoints for conversational cultural companion grounded in VieCultures library")
public class CompanionChatController {

    private final CompanionChatService companionChatService;
    private final ObjectMapper objectMapper;

    @PostMapping("/chat")
    @Operation(
            summary = "Send chat message to Cultural Companion",
            description = "Processes conversational query grounded in lesson context or global article search. Free tier limited to 3 turns/day."
    )
    public ResponseEntity<ApiResponse<CompanionChatResponse>> chat(
            @Valid @RequestBody CompanionChatRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        String clientIdentifier = resolveClientIdentifier(authentication, servletRequest, null);
        boolean isPro = checkProStatus(authentication);

        CompanionChatResponse response = companionChatService.chat(request, clientIdentifier, isPro);
        return ResponseEntity.ok(ApiResponse.ok(response, "Companion response generated successfully"));
    }

    @PostMapping("/translate")
    @Operation(
            summary = "Cultural inline translation",
            description = "Translates text between Vietnamese and English preserving cultural nuances, folklore terms, and culinary context"
    )
    public ResponseEntity<ApiResponse<CompanionTranslateResponse>> translate(
            @Valid @RequestBody CompanionTranslateRequest request
    ) {
        log.info("Translating text (length: {}) from {} to {}",
                request.getText() != null ? request.getText().length() : 0,
                request.getSourceLang(),
                request.getTargetLang());
        CompanionTranslateResponse response = companionChatService.translate(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Text translated successfully"));
    }

    @GetMapping("/history")
    @Operation(
            summary = "Get companion chat history",
            description = "Retrieves stored conversation turns for current user/client, optionally filtered by lessonId"
    )
    public ResponseEntity<ApiResponse<List<CompanionChatMessage>>> getHistory(
            @RequestParam(value = "lessonId", required = false) String lessonId,
            @RequestParam(value = "clientIdentifier", required = false) String clientIdentifierParam,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        String clientIdentifier = resolveClientIdentifier(authentication, servletRequest, clientIdentifierParam);
        List<CompanionChatMessage> history = companionChatService.getChatHistory(clientIdentifier, lessonId);
        return ResponseEntity.ok(ApiResponse.ok(history, "Companion chat history retrieved successfully"));
    }

    @PostMapping("/history")
    @Operation(
            summary = "Save companion chat history",
            description = "Persists one or more chat messages associated with the client and optional lesson"
    )
    public ResponseEntity<ApiResponse<List<CompanionChatMessage>>> saveHistory(
            @RequestBody JsonNode requestNode,
            @RequestParam(value = "lessonId", required = false) String lessonIdParam,
            @RequestParam(value = "clientIdentifier", required = false) String clientIdentifierParam,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        String clientIdentifier = resolveClientIdentifier(authentication, servletRequest, clientIdentifierParam);
        String lessonId = lessonIdParam;
        List<CompanionChatMessage> messages = new ArrayList<>();

        try {
            if (requestNode.isArray()) {
                for (JsonNode item : requestNode) {
                    messages.add(objectMapper.treeToValue(item, CompanionChatMessage.class));
                }
            } else if (requestNode.isObject()) {
                if (requestNode.has("lessonId") && !requestNode.get("lessonId").isNull()) {
                    lessonId = requestNode.get("lessonId").asText();
                }
                if (requestNode.has("clientIdentifier") && !requestNode.get("clientIdentifier").isNull() && (clientIdentifierParam == null || clientIdentifierParam.isBlank())) {
                    clientIdentifier = requestNode.get("clientIdentifier").asText();
                }

                if (requestNode.has("messages") && requestNode.get("messages").isArray()) {
                    for (JsonNode item : requestNode.get("messages")) {
                        messages.add(objectMapper.treeToValue(item, CompanionChatMessage.class));
                    }
                } else if (requestNode.has("content")) {
                    CompanionChatMessage singleMsg = objectMapper.treeToValue(requestNode, CompanionChatMessage.class);
                    messages.add(singleMsg);
                }
            }
        } catch (Exception ex) {
            log.error("Failed to parse companion history payload: {}", ex.getMessage(), ex);
        }

        List<CompanionChatMessage> saved = companionChatService.saveChatHistory(clientIdentifier, lessonId, messages);
        return ResponseEntity.ok(ApiResponse.ok(saved, "Chat history saved successfully"));
    }

    @DeleteMapping("/history")
    @Operation(
            summary = "Clear companion chat history",
            description = "Deletes chat history for current user/client, optionally filtered by lessonId"
    )
    public ResponseEntity<ApiResponse<Void>> deleteHistory(
            @RequestParam(value = "lessonId", required = false) String lessonId,
            @RequestParam(value = "clientIdentifier", required = false) String clientIdentifierParam,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        String clientIdentifier = resolveClientIdentifier(authentication, servletRequest, clientIdentifierParam);
        companionChatService.clearChatHistory(clientIdentifier, lessonId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Companion chat history cleared successfully"));
    }

    private String resolveClientIdentifier(Authentication authentication, HttpServletRequest servletRequest, String overrideIdentifier) {
        if (overrideIdentifier != null && !overrideIdentifier.isBlank()) {
            return overrideIdentifier.trim();
        }
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        String forwardedFor = servletRequest.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return servletRequest.getRemoteAddr();
    }

    private boolean checkProStatus(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String role = authority.getAuthority();
                if ("ROLE_PRO".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role) || "ROLE_PREMIUM".equalsIgnoreCase(role)) {
                    return true;
                }
            }
        }
        return false;
    }
}
