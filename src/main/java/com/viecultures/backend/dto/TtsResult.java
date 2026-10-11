package com.viecultures.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsResult {
    private byte[] audioData;
    private String provider;
    private boolean fromCache;
    private String contentType;
}
