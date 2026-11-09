package com.viecultures.backend.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningJourneyRequest {
    private String id; // Optional custom slug/id. If blank, can be auto-derived from titleEn
    private String titleEn;
    private String titleVi;
    private String theme; // cuisine, places, crafts, festivals, history
    private String provinceId;
    private String region; // north, central, south
    private String description;
    private String descriptionVi;
    private String imageUrl;
    private String status; // published, draft, upcoming, scheduled
    private LocalDateTime scheduledPublishAt; // Scheduled time for automated publishing
    private List<String> chapterPostIds; // Ordered list of ReadingPost IDs to assign to this journey (chặng 1, 2, 3...)
}
