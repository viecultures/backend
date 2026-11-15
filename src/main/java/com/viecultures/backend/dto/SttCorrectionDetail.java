package com.viecultures.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chi tiết chỉnh sửa trong bước Semantic Refinement (Pass 2) của Two-Pass STT.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SttCorrectionDetail {

    private String original;

    private String replacement;

    /**
     * Loại chỉnh sửa: FILLER_WORD | SELF_CORRECTION | HOMOPHONE | PUNCTUATION
     */
    private String type;

    private String explanation;
}
