package com.viecultures.backend.modules.admin.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPromptConfigDto {
    private String key;
    private String name;
    private String description;
    private String category;
    private String currentPrompt;
    private String defaultPrompt;
    private boolean isModified;
    private List<AiPromptVariableDto> supportedVariables;
    private LocalDateTime updatedAt;
}
