package com.viecultures.backend.dto.streak;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SealLessonRequest {

    @NotBlank(message = "readingPostId cannot be blank")
    private String readingPostId;

    @Builder.Default
    private Integer wordsLearnedCount = 0;

    @Builder.Default
    private String source = "reader";
}
