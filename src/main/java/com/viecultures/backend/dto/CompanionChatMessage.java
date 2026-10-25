package com.viecultures.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Message entity within a conversation turn")
public class CompanionChatMessage {

    @Schema(description = "Unique message ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "Role of the speaker: user or assistant", example = "user")
    private String role;

    @Schema(description = "Text content of the message", example = "Can you explain Pho?")
    private String content;

    @Schema(description = "Translated content in counterpart language", example = "Ban co the giai thich ve Pho khong?")
    private String translatedContent;

    @Schema(description = "Optional lesson ID associated with this message", example = "550e8400-e29b-41d4-a716-446655440000")
    private String lessonId;

    @Schema(description = "Timestamp when message was created")
    private LocalDateTime createdAt;
}
