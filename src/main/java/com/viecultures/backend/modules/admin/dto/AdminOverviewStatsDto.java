package com.viecultures.backend.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOverviewStatsDto {
    private long totalUsers;
    private long activeLearnersWeek;
    private long totalPosts;
    private long publishedPosts;
    private long totalReads;
    private double avgReadsPerPost;
    private Map<String, Long> cefrCoverage;
    private Map<String, Long> regionCoverage;
    private String systemStatus;
}
