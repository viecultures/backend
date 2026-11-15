package com.viecultures.backend.dto;

import com.viecultures.backend.entity.CefrLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogPostView {
    private String id;
    private String topicGroup;
    private String titleEn;
    private String titleVi;
    private String topic;
    private String region;
    private String imageUrl;
    private Integer wordCount;
    private String summary;
    private String summaryVi;
    private CefrLevel level;
    private String readTime;
    private Integer totalReads;
    private Integer likesCount;
    private Boolean featured;
    private LocalDateTime createdAt;
    private String journeyId;
}
