package com.viecultures.backend.modules.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanDto {
    private String id;
    private String code;
    private String nameVi;
    private String nameEn;
    private String descriptionVi;
    private Long priceVnd;
    private Integer discountPercent;
    private String billingCycle;
    private Boolean isActive;
    private Boolean isPopularBadge;
    private Integer sortOrder;
    private PlanFeatureEntitlementsDto entitlements;
    @Builder.Default
    private List<String> perksVi = new ArrayList<>();
    @Builder.Default
    private List<String> perksEn = new ArrayList<>();
}
