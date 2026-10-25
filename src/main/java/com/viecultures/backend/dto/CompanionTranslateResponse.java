package com.viecultures.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response payload for cultural inline translation")
public class CompanionTranslateResponse {

    @Schema(description = "Translated text preserving cultural nuances", example = "Square sticky rice cake expresses gratitude to heaven, earth, and ancestors.")
    private String translatedText;
}
