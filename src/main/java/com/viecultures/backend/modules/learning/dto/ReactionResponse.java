package com.viecultures.backend.modules.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionResponse {
    private String reflectionId;
    private Integer reactionsCount;
    private boolean userLiked;
}
