package com.viecultures.backend.modules.gamification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreakSyncResponse {
    private UserStreakDto streak;
    private Boolean isNewDay;
    private String status;
    private String message;
}
