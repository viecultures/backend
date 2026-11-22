package com.viecultures.backend.modules.chat.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viecultures.backend.common.response.ApiResponse;
import com.viecultures.backend.dto.*;
import com.viecultures.backend.modules.chat.dto.*;
import com.viecultures.backend.entity.SubscriptionTransaction;
import com.viecultures.backend.repository.SubscriptionTransactionRepository;
import com.viecultures.backend.modules.chat.service.CompanionChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    private final SubscriptionTransactionRepository subscriptionTransactionRepository;

    @PostMapping("/chat")
    @Operation(
            summary = "Send chat message to Cultural Companion",
            description = "Processes conversational query grounded in lesson context or global article search. Free tier limited to 5 turns/day."
    )
    public ResponseEntity<ApiResponse<CompanionChatResponse>> chat(
            @Valid @RequestBody CompanionChatRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH_REQUIRED", "Please log in to chat with the Cultural Companion"));
        }
        String clientIdentifier = authentication.getName();
        boolean isPro = checkProStatus(authentication);

        try {
            CompanionChatResponse response = companionChatService.chat(request, clientIdentifier, isPro);
            return ResponseEntity.ok(ApiResponse.ok(response, "Companion response generated successfully"));
        } catch (org.springframework.web.server.ResponseStatusException rse) {
            throw rse;
        } catch (Exception ex) {
            log.error("Unhandled error in companion chat for client {}: {}", clientIdentifier, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("COMPANION_ERROR", "Internal Server Error: " + ex.getMessage()));
        }
    }

    @GetMapping("/quota")
    @Operation(summary = "Get companion chat quota for current authenticated user")
    public ResponseEntity<ApiResponse<CompanionQuotaResponse>> getQuota(
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH_REQUIRED", "Please log in to view quota"));
        }
        String clientIdentifier = authentication.getName();
        boolean isPro = checkProStatus(authentication);
        int remaining = companionChatService.getRemainingTurns(clientIdentifier, isPro);
        return ResponseEntity.ok(ApiResponse.ok(
                new CompanionQuotaResponse(5, remaining, isPro),
                "Quota retrieved successfully"
        ));
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
            description = "Retrieves stored conversation turns for current user, optionally filtered by lessonId"
    )
    public ResponseEntity<ApiResponse<List<CompanionChatMessage>>> getHistory(
            @RequestParam(value = "lessonId", required = false) String lessonId,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH_REQUIRED", "Please log in to chat with the Cultural Companion"));
        }
        String clientIdentifier = authentication.getName();
        List<CompanionChatMessage> history = companionChatService.getChatHistory(clientIdentifier, lessonId);
        return ResponseEntity.ok(ApiResponse.ok(history, "Companion chat history retrieved successfully"));
    }

    @PostMapping("/history")
    @Operation(
            summary = "Save companion chat history",
            description = "Persists one or more chat messages associated with the user and optional lesson"
    )
    public ResponseEntity<ApiResponse<List<CompanionChatMessage>>> saveHistory(
            @RequestBody JsonNode requestNode,
            @RequestParam(value = "lessonId", required = false) String lessonIdParam,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH_REQUIRED", "Please log in to chat with the Cultural Companion"));
        }
        String clientIdentifier = authentication.getName();
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
            description = "Deletes chat history for current user, optionally filtered by lessonId"
    )
    public ResponseEntity<ApiResponse<Void>> deleteHistory(
            @RequestParam(value = "lessonId", required = false) String lessonId,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH_REQUIRED", "Please log in to chat with the Cultural Companion"));
        }
        String clientIdentifier = authentication.getName();
        companionChatService.clearChatHistory(clientIdentifier, lessonId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Companion chat history cleared successfully"));
    }

    private boolean checkProStatus(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String role = authority.getAuthority();
                if ("ROLE_PRO".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role) || "ROLE_PREMIUM".equalsIgnoreCase(role)) {
                    return true;
                }
            }

            if (subscriptionTransactionRepository != null) {
                try {
                    String identifier = authentication.getName();
                    List<SubscriptionTransaction> txList = subscriptionTransactionRepository
                            .findByUserIdOrUserEmailOrderByCreatedAtDesc(identifier, identifier);
                    boolean hasApprovedSubscription = txList.stream()
                            .anyMatch(t -> "APPROVED".equalsIgnoreCase(t.getStatus()));
                    if (hasApprovedSubscription) {
                        return true;
                    }
                } catch (Exception ex) {
                    log.warn("Failed to check subscription status for {}: {}", authentication.getName(), ex.getMessage());
                }
            }
        }
        return false;
    }
}
