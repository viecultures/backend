package com.viecultures.backend.service;

import com.viecultures.backend.entity.AiUsageLog;

import java.math.BigDecimal;

public interface AiCostTrackingService {

    AiUsageLog recordUsage(
            Long configId,
            String provider,
            String featureCode,
            String modelName,
            int promptTokens,
            int completionTokens,
            int characterCount,
            int latencyMs,
            int httpStatus
    );

    BigDecimal calculateEstimatedCost(
            String provider,
            String modelName,
            int promptTokens,
            int completionTokens,
            int characterCount
    );
}
