package com.viecultures.backend.modules.gamification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreakSyncRequest {
    /**
     * Optional target date for synchronization (defaults to current date if null).
     */
    private LocalDate targetDate;
}
