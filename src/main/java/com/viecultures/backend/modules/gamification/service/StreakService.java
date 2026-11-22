package com.viecultures.backend.modules.gamification.service;

import com.viecultures.backend.modules.gamification.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface StreakService {

    /**
     * Retrieves current streak state and total seals for the authenticated user.
     */
    UserStreakDto getUserStreak(String userEmail);

    /**
     * Synchronizes and increments the daily streak based on calendar day difference logic.
     */
    StreakSyncResponse syncStreak(String userEmail, LocalDate targetDate);

    /**
     * Persists Mộc Son Red Seal stamp for a completed lesson and triggers daily streak activation.
     */
    SealLessonResponse sealLesson(String userEmail, SealLessonRequest request);

    /**
     * Retrieves all seals unlocked by the authenticated user.
     */
    List<UserSealDto> getUserSeals(String userEmail);
}
