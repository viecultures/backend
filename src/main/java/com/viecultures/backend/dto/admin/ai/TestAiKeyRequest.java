package com.viecultures.backend.dto.admin.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestAiKeyRequest {

    private Long configId;
    private String provider;
    private String serviceType;
    private String apiKey;
}
