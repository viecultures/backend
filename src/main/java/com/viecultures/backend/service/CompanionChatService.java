package com.viecultures.backend.service;

import com.viecultures.backend.dto.CompanionChatRequest;
import com.viecultures.backend.dto.CompanionChatResponse;

public interface CompanionChatService {

    /**
     * Executes conversational turn with AI Cultural Companion.
     *
     * @param request          Request containing current query, optional lessonId, and dialogue history
     * @param clientIdentifier Unique identifier for rate-limiting (User ID or Remote IP)
     * @param isPro            Whether the client holds an active Pro/Trial subscription
     * @return Formatted companion response with grounding metadata and remaining quota
     */
    CompanionChatResponse chat(CompanionChatRequest request, String clientIdentifier, boolean isPro);
}
