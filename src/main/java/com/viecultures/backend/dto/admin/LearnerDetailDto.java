package com.viecultures.backend.dto.admin;

import com.viecultures.backend.entity.CefrLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearnerDetailDto {

    // User Profile
    private String id;
    private String name;
    private String email;
    private String avatarUrl;
    private String role;
    private String authProvider;
    private boolean enabled;
    private CefrLevel englishLevel;
    private LocalDateTime createdAt;

    // Subscription
    private String currentPlanCode;
    private String currentPlanName;
    private String subscriptionStatus;
    private LocalDateTime subscriptionExpiry;
    private List<SubscriptionTransactionDto> transactions;

    // Learning Stats
    private Integer streakDays;
    private LocalDate lastActiveDate;
    private long totalSealedLessons;
    private long totalWordsLearned;
    private long learningVocabCount;
    private long masteredVocabCount;

    // Completed Lessons
    private List<LearnerCompletedLessonDto> completedLessons;
}
