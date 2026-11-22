package com.viecultures.backend.modules.gamification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityStatsSummaryDto {

    private long totalLogs;

    @Builder.Default
    private Map<String, Long> categoryCounts = new HashMap<>();

    @Builder.Default
    private Map<String, Long> eventTypeCounts = new HashMap<>();

    @Builder.Default
    private List<TopMetricItemDto> topReadPosts = new ArrayList<>();

    @Builder.Default
    private List<TopMetricItemDto> topLookedUpWords = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopMetricItemDto {
        private String key;
        private String label;
        private long count;
    }
}
