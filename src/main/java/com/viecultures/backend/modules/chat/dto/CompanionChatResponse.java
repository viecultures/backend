package com.viecultures.backend.modules.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Response payload from AI Companion")
public class CompanionChatResponse {

    @Schema(description = "Companion textual explanation or response", example = "In Hanoi, eating Pho with quay...")
    private String reply;

    @Builder.Default
    @Schema(description = "Quick suggested prompts to continue the conversation")
    private List<String> suggestedPrompts = new ArrayList<>();

    @Builder.Default
    @Schema(description = "Cultural articles referenced for grounding")
    private List<ReferencedLessonDto> referencedLessons = new ArrayList<>();

    @Schema(description = "Remaining chat turns for today under free tier (-1 for unlimited/pro)", example = "2")
    private Integer remainingDailyTurns;
}
