package com.viecultures.backend.modules.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVocabProgressDto {
    private String userId;
    private String vocabId;
    private String word;
    private String status; // LEARNING, MASTERED
    private Integer reviewCount;
    private String contextSentence;
    private LocalDateTime lastReviewedAt;
}
