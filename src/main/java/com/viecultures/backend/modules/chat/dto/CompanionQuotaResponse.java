package com.viecultures.backend.modules.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("isPro")
    private boolean isPro;
}
