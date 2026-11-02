package com.viecultures.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_usage_logs", indexes = {
        @Index(name = "idx_ai_log_created_at", columnList = "created_at"),
        @Index(name = "idx_ai_log_provider", columnList = "provider"),
        @Index(name = "idx_ai_log_feature", columnList = "feature_code"),
        @Index(name = "idx_ai_log_config_id", columnList = "config_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_id")
    private Long configId;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "feature_code", nullable = false, length = 50)
    private String featureCode;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Builder.Default
    @Column(name = "prompt_tokens", nullable = false)
    private int promptTokens = 0;

    @Builder.Default
    @Column(name = "completion_tokens", nullable = false)
    private int completionTokens = 0;

    @Builder.Default
    @Column(name = "character_count", nullable = false)
    private int characterCount = 0;

    @Column(name = "estimated_cost_usd", precision = 10, scale = 6)
    private BigDecimal estimatedCostUsd;

    @Builder.Default
    @Column(name = "latency_ms", nullable = false)
    private int latencyMs = 0;

    @Builder.Default
    @Column(name = "http_status", nullable = false)
    private int httpStatus = 200;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
