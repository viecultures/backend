package com.viecultures.backend.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminVocabRequest {

    @NotBlank(message = "Từ vựng không được để trống")
    @Size(max = 120, message = "Từ vựng không vượt quá 120 ký tự")
    private String word;

    @Size(max = 60, message = "Từ loại không vượt quá 60 ký tự")
    private String pos;

    @Size(max = 100, message = "Phiên âm IPA không vượt quá 100 ký tự")
    private String ipa;

    private String viMeaning;

    private String enDefinition;

    private String contextSentence;

    @Size(max = 20, message = "Cấp độ không vượt quá 20 ký tự")
    private String level;

    private String audioUrl;

    private String postId;

    private String usageNote;
}
