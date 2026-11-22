package com.viecultures.backend.modules.admin.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAiKeyRequest {

    private String keyLabel;
    private String serviceType;
    private String apiKey;
    private String modelName;
    private String region;
    private String extraConfig;
    private Integer priority;
    private Boolean active;
    private String status;
}
