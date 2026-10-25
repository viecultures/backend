package com.viecultures.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Message entity within a conversation turn")
public class CompanionChatMessage {

    @Schema(description = "Role of the speaker: user or assistant", example = "user")
    private String role;

    @Schema(description = "Text content of the message", example = "Can you explain Pho?")
    private String content;
}
