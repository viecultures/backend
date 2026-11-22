package com.viecultures.backend.modules.gamification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SealLessonResponse {
    private UserSealDto seal;
    private Boolean isNewSeal;
    private StreakSyncResponse streak;
    private String mascotMessage;
}
