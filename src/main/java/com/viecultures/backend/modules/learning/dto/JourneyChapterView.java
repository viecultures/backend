package com.viecultures.backend.modules.learning.dto;

import com.viecultures.backend.entity.CefrLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight projection for ReadingPost chapter metadata inside LearningJourney listings.
 * Avoids loading heavy TEXT columns (contentEn, contentVi, audioTracks) and lazy associations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JourneyChapterView {
    private String id;
    private String journeyId;
    private Integer chapterOrder;
    private String topicGroup;
    private String titleEn;
    private String titleVi;
    private String summary;
    private String readTime;
    private CefrLevel level;
    private String imageUrl;
}
