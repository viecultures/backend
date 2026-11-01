package com.viecultures.backend.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningJourneyResponse {
    private String id;
    private String titleEn;
    private String titleVi;
    private String theme;
    private String provinceId;
    private String region;
    private String description;
    private String imageUrl;
    private String status;
    private LocalDateTime createdAt;
    private List<ChapterSummaryDto> chapters;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChapterSummaryDto {
        private String id;
        private Integer chapterOrder;
        private String titleEn;
        private String titleVi;
        private String summary;
        private String readTime;
        private String level;
        private String imageUrl;
    }
}
