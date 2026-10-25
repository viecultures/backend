package com.viecultures.backend.service;

import com.viecultures.backend.dto.CompanionChatMessage;
import com.viecultures.backend.dto.CompanionChatRequest;
import com.viecultures.backend.dto.CompanionChatResponse;
import com.viecultures.backend.dto.CompanionTranslateRequest;
import com.viecultures.backend.dto.CompanionTranslateResponse;

import java.util.List;

public interface CompanionChatService {

    /**
     * Executes conversational turn with AI Cultural Companion.
     *
     * @param request          Request containing current query, optional lessonId, language, and dialogue history
     * @param clientIdentifier Unique identifier for rate-limiting (User ID or Remote IP)
     * @param isPro            Whether the client holds an active Pro/Trial subscription
     * @return Formatted companion response with grounding metadata and remaining quota
     */
    CompanionChatResponse chat(CompanionChatRequest request, String clientIdentifier, boolean isPro);

    /**
     * Translates text between Vietnamese and English preserving cultural nuances.
     *
     * @param request Request containing text, source language, and target language
     * @return Translated text
     */
    CompanionTranslateResponse translate(CompanionTranslateRequest request);

    /**
     * Retrieves stored conversation turns for a client and optional lesson.
     *
     * @param clientIdentifier User ID or client identifier
     * @param lessonId         Optional lesson ID
     * @return List of chat messages in chronological order
     */
    List<CompanionChatMessage> getChatHistory(String clientIdentifier, String lessonId);

    /**
     * Saves one or more conversation turns to database.
     *
     * @param clientIdentifier User ID or client identifier
     * @param lessonId         Optional lesson ID
     * @param messages         List of chat messages to persist
     * @return List of persisted chat messages with generated IDs
     */
    List<CompanionChatMessage> saveChatHistory(String clientIdentifier, String lessonId, List<CompanionChatMessage> messages);

    /**
     * Deletes stored chat history for a client and optional lesson.
     *
     * @param clientIdentifier User ID or client identifier
     * @param lessonId         Optional lesson ID
     */
    void clearChatHistory(String clientIdentifier, String lessonId);
}
