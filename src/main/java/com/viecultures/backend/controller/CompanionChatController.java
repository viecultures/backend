package com.viecultures.backend.controller;

import com.viecultures.backend.dto.ApiResponse;
import com.viecultures.backend.dto.CompanionChatRequest;
import com.viecultures.backend.dto.CompanionChatResponse;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/companion")
@RequiredArgsConstructor
@Tag(name = "AI Companion", description = "Endpoints for conversational cultural companion grounded in VieCultures library")
public class CompanionChatController {

    private final CompanionChatService companionChatService;

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
        String clientIdentifier;
        boolean isPro = false;

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            clientIdentifier = authentication.getName();
            // Check if user has PRO or ADMIN authority
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String role = authority.getAuthority();
                if ("ROLE_PRO".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role) || "ROLE_PREMIUM".equalsIgnoreCase(role)) {
                    isPro = true;
                    break;
                }
            }
        } else {
            // Use remote IP address for anonymous visitors
            String forwardedFor = servletRequest.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                clientIdentifier = forwardedFor.split(",")[0].trim();
            } else {
                clientIdentifier = servletRequest.getRemoteAddr();
            }
        }

        CompanionChatResponse response = companionChatService.chat(request, clientIdentifier, isPro);
        return ResponseEntity.ok(ApiResponse.ok(response, "Companion response generated successfully"));
    }
}
