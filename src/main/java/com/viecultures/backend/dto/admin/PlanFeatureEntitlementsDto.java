package com.viecultures.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanFeatureEntitlementsDto {
    private Integer maxAiAudioMinutesPerMonth;
    private String maxCefrLevelAccess;
    private Boolean unlimitedJourneys;
    private Boolean offlineDownloadsAllowed;
    private Boolean highlightBadge;
}
