package com.viecultures.backend.modules.admin.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestAiKeyResponse {

    private boolean success;
    private long latencyMs;
    private int httpStatus;
    private String message;
    private String modelUsed;
}
