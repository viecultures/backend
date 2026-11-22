package com.viecultures.backend.modules.learning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReflectionRequest {

    @NotBlank(message = "postId is required")
    private String postId;

    @NotBlank(message = "Content cannot be blank")
    @Size(min = 10, message = "Reflection content must be at least 10 characters long")
    private String content;

    @Builder.Default
    private List<String> usedVocabIds = new ArrayList<>();
}
