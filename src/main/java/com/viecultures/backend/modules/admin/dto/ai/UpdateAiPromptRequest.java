package com.viecultures.backend.modules.admin.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAiPromptRequest {

    @NotBlank(message = "Nội dung prompt không được để trống")
    private String promptText;
}
