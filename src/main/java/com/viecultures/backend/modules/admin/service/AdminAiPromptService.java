package com.viecultures.backend.modules.admin.service;

import com.viecultures.backend.modules.admin.dto.ai.AiPromptConfigDto;
import com.viecultures.backend.modules.admin.dto.ai.UpdateAiPromptRequest;

import java.util.List;

public interface AdminAiPromptService {

    List<AiPromptConfigDto> getAllPrompts();

    AiPromptConfigDto getPromptByKey(String key);

    AiPromptConfigDto updatePrompt(String key, UpdateAiPromptRequest request);

    AiPromptConfigDto resetPrompt(String key);

    String getResolvedPrompt(String key, String defaultFallback);
}
