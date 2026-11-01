package com.viecultures.backend.dto.streak;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSealDto {
    private String id;
    private String readingPostId;
    private LocalDateTime sealedAt;
    private LocalDate sealedDate;
    private Integer wordsLearnedCount;
    private String source;
}
