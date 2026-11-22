package com.viecultures.backend.modules.admin.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiServiceConfigDto {

    private Long id;
    private String provider;
    private String serviceType;
    private String keyLabel;
    private String maskedKey;
    private String modelName;
    private String region;
    private String extraConfig;
    private boolean active;
    private int priority;
    private String status;
    private LocalDateTime cooldownUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
