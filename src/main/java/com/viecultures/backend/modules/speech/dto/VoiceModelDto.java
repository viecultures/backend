package com.viecultures.backend.modules.speech.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin model giọng đọc AI")
public class VoiceModelDto {

    @Schema(description = "Mã định danh model giọng đọc", example = "en-US-AmandaMultilingualNeural")
    private String id;

    @Schema(description = "Tên hiển thị", example = "Amanda (Nữ - Đa ngôn ngữ)")
    private String name;

    @Schema(description = "Giới tính", example = "Female")
    private String gender;

    @Schema(description = "Vùng / Accent", example = "en-US")
    private String locale;

    @Schema(description = "Mô tả chất giọng và phong cách phát âm", example = "Giọng Nữ truyền cảm, phát âm tự nhiên, đa ngôn ngữ chuẩn xác")
    private String description;

    @Schema(description = "Là model Multilingual cao cấp", example = "true")
    private boolean isMultilingual;

    @Schema(description = "Là giọng gợi ý hàng đầu", example = "true")
    private boolean isPrimary;
}
