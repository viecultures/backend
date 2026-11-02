package com.viecultures.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "subscription_plans", indexes = {
    @Index(name = "idx_subscription_plan_code", columnList = "code", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // FREE, STARTER, PRO, LIFETIME

    @Column(name = "name_vi", nullable = false)
    private String nameVi;

    @Column(name = "name_en", nullable = false)
    private String nameEn;

    @Column(name = "description_vi", columnDefinition = "TEXT")
    private String descriptionVi;

    @Column(name = "price_vnd", nullable = false)
    private Long priceVnd;

    @Builder.Default
    @Column(name = "discount_percent", nullable = false)
    private Integer discountPercent = 0;

    @Builder.Default
    @Column(name = "billing_cycle", nullable = false, length = 30)
    private String billingCycle = "MONTHLY"; // MONTHLY, QUARTERLY, ANNUAL, ONE_TIME

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_popular_badge", nullable = false)
    private Boolean isPopularBadge = false;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    // Feature entitlements
    @Builder.Default
    @Column(name = "max_ai_audio_minutes_per_month", nullable = false)
    private Integer maxAiAudioMinutesPerMonth = 30;

    @Builder.Default
    @Column(name = "max_cefr_level_access", nullable = false, length = 10)
    private String maxCefrLevelAccess = "B1";

    @Builder.Default
    @Column(name = "unlimited_journeys", nullable = false)
    private Boolean unlimitedJourneys = false;

    @Builder.Default
    @Column(name = "offline_downloads_allowed", nullable = false)
    private Boolean offlineDownloadsAllowed = false;

    @Builder.Default
    @Column(name = "highlight_badge", nullable = false)
    private Boolean highlightBadge = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "subscription_plan_perks_vi", joinColumns = @JoinColumn(name = "plan_id"))
    @Column(name = "perk", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> perksVi = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "subscription_plan_perks_en", joinColumns = @JoinColumn(name = "plan_id"))
    @Column(name = "perk", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> perksEn = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
