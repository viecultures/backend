package com.viecultures.backend.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogLessonDto {
    private String id;
    private String journeyId;
    private Boolean isStandalone;
    private Map<String, String> postIds; // Keyed by "Level 1", "Level 2", "Level 3"
    private String topicGroup;
    private String titleEn;
    private String titleVi;
    private String category;    // "Heritage" | "Cuisine" | "Crafts" | "Nature" | "Folklore"
    private String categoryVi;  // Vietnamese label, e.g. "Di sản", "Ẩm thực"
    private String region;
    private String imageUrl;
    private String summary;
    private Boolean featured;
    private Integer totalReads;
    private Integer likesCount;
    private String primaryLevel; // "Level 1" | "Level 2" | "Level 3"
    private String readTime;
    private Integer vocabCount;
    private List<String> availableLevels; // e.g. ["Level 1", "Level 2", "Level 3"]
    private Map<String, LevelDetailDto> levelDetails; // Keyed by "Level 1", "Level 2", "Level 3"
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelDetailDto {
        private String postId;
        private String cefrLevel;
        private String readTime;
        private Integer vocabCount;
        private Integer wordCount;
        private String summary;
    }
}
