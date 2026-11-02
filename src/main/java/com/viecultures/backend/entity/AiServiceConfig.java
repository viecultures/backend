package com.viecultures.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_service_configs", indexes = {
        @Index(name = "idx_ai_cfg_provider_type", columnList = "provider, service_type, is_active"),
        @Index(name = "idx_ai_cfg_priority", columnList = "priority"),
        @Index(name = "idx_ai_cfg_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiServiceConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "service_type", nullable = false, length = 50)
    private String serviceType;

    @Column(name = "key_label", nullable = false, length = 100)
    private String keyLabel;

    @Column(name = "encrypted_key", nullable = false, columnDefinition = "TEXT")
    private String encryptedKey;

    @Column(name = "masked_key", nullable = false, length = 100)
    private String maskedKey;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "region", length = 50)
    private String region;

    @Column(name = "extra_config", columnDefinition = "TEXT")
    private String extraConfig;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "priority", nullable = false)
    private int priority = 1;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private String status = "HEALTHY";

    @Column(name = "cooldown_until")
    private LocalDateTime cooldownUntil;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
