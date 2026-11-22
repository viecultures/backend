package com.viecultures.backend.modules.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minimal option projection for dropdowns and selectors (id, titleVi, titleEn).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityOptionDto {
    private String id;
    private String titleVi;
    private String titleEn;
}
