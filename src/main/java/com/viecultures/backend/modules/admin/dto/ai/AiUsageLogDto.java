package com.viecultures.backend.modules.admin.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageLogDto {

    private Long id;
    private Long configId;
    private String provider;
    private String featureCode;
    private String modelName;
    private int promptTokens;
    private int completionTokens;
    private int characterCount;
    private BigDecimal estimatedCostUsd;
    private int latencyMs;
    private int httpStatus;
    private LocalDateTime createdAt;
}
