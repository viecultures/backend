package com.viecultures.backend.modules.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for cultural inline translation")
public class CompanionTranslateRequest {

    @NotBlank(message = "Text to translate must not be blank")
    @Schema(description = "Original text to translate", example = "Banh chung the hien long biet on dat troi va to tien.")
    private String text;

    @Schema(description = "Source language code (e.g., 'vi', 'en')", example = "vi")
    private String sourceLang;

    @Schema(description = "Target language code (e.g., 'en', 'vi')", example = "en")
    private String targetLang;
}
