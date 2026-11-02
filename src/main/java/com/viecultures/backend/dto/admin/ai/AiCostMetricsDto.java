package com.viecultures.backend.dto.admin.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCostMetricsDto {

    private BigDecimal totalCostUsd;
    private BigDecimal todayCostUsd;
    private BigDecimal thisMonthCostUsd;
    private long totalRequests;
    private long todayRequests;
    private long totalTokens;
    private long totalCharacters;

    @Builder.Default
    private List<ProviderCostBreakdownDto> providerBreakdown = new ArrayList<>();

    @Builder.Default
    private List<FeatureCostBreakdownDto> featureBreakdown = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderCostBreakdownDto {
        private String provider;
        private BigDecimal totalCostUsd;
        private long requestCount;
        private long totalTokens;
        private long totalCharacters;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureCostBreakdownDto {
        private String featureCode;
        private BigDecimal totalCostUsd;
        private long requestCount;
    }
}
