package com.viecultures.backend.modules.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionMetricsDto {
    private Long totalRevenueVnd;
    private Long pendingCount;
    private Long activeSubscribers;
    private Double approvalRatePercent;
    private Map<String, Long> tierDistribution;
}
