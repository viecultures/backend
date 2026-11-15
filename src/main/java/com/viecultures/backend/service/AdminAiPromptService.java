package com.viecultures.backend.service;

import com.viecultures.backend.dto.admin.ai.AiPromptConfigDto;
import com.viecultures.backend.dto.admin.ai.UpdateAiPromptRequest;

import java.util.List;

public interface AdminAiPromptService {

    List<AiPromptConfigDto> getAllPrompts();

    AiPromptConfigDto getPromptByKey(String key);

    AiPromptConfigDto updatePrompt(String key, UpdateAiPromptRequest request);

    AiPromptConfigDto resetPrompt(String key);

    String getResolvedPrompt(String key, String defaultFallback);
}
