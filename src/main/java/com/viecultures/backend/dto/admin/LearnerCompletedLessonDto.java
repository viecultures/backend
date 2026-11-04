package com.viecultures.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearnerCompletedLessonDto {
    private String readingPostId;
    private String titleEn;
    private String titleVi;
    private LocalDateTime sealedAt;
    private Integer wordsLearnedCount;
}
