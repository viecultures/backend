package com.viecultures.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for AI Companion chat turn")
public class CompanionChatRequest {

    @Schema(description = "Optional ID of current reading lesson for in-context grounding", example = "550e8400-e29b-41d4-a716-446655440000")
    private String lessonId;

    @NotBlank(message = "Message must not be blank")
    @Schema(description = "User's query or prompt", example = "Why do people eat Pho with quay?")
    private String message;

    @Builder.Default
    @Schema(description = "Previous dialogue turns in chronological order")
    private List<CompanionChatMessage> history = new ArrayList<>();
}
