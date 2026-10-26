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
public class UserStreakDto {
    private Integer streakDays;
    private LocalDate lastActiveDate;
    private Boolean streakProtected;
    private Integer streakFreezeCount;
    private LocalDateTime updatedAt;
    private Long totalSeals;
}
