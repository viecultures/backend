package com.viecultures.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanionQuotaResponse {
    private int dailyLimit;
    private int remainingTurns;
    private boolean isPro;
}
