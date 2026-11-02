package com.viecultures.backend.dto.admin.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAiKeyRequest {

    @NotBlank(message = "Nha cung cap AI khong duoc de trong (GEMINI hoac AZURE)")
    private String provider;

    @NotBlank(message = "Loai dich vu khong duoc de trong (CHAT, TTS hoac TRANSLATE)")
    private String serviceType;

    @NotBlank(message = "Nhan khoa khong duoc de trong")
    private String keyLabel;

    @NotBlank(message = "API key khong duoc de trong")
    private String apiKey;

    @Builder.Default
    private Integer priority = 1;

    @Builder.Default
    private Boolean active = true;
}
