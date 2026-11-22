package com.viecultures.backend.modules.gamification.service.impl;

import com.viecultures.backend.modules.gamification.dto.*;
import com.viecultures.backend.entity.User;
import com.viecultures.backend.entity.UserSeal;
import com.viecultures.backend.entity.UserStreak;
import com.viecultures.backend.repository.UserSealRepository;
import com.viecultures.backend.repository.UserStreakRepository;
import com.viecultures.backend.repository.UserRepository;
import com.viecultures.backend.modules.gamification.service.StreakService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreakServiceImpl implements StreakService {

    private final UserRepository userRepository;
    private final UserStreakRepository userStreakRepository;
    private final UserSealRepository userSealRepository;

    @Override
    @Transactional(readOnly = true)
    public UserStreakDto getUserStreak(String userEmail) {
        User user = findUser(userEmail);
        long totalSeals = userSealRepository.countByUserId(user.getId());

        UserStreak streak = userStreakRepository.findByUserId(user.getId())
                .orElse(null);

        if (streak == null) {
            return UserStreakDto.builder()
                    .streakDays(0)
                    .lastActiveDate(null)
                    .streakProtected(false)
                    .streakFreezeCount(1)
                    .updatedAt(null)
                    .totalSeals(totalSeals)
                    .build();
        }

        return toDto(streak, totalSeals);
    }

    @Override
    @Transactional
    public StreakSyncResponse syncStreak(String userEmail, LocalDate targetDate) {
        User user = findUser(userEmail);
        LocalDate activeDate = targetDate != null ? targetDate : LocalDate.now();

        UserStreak streak = userStreakRepository.findByUserId(user.getId())
                .orElseGet(() -> UserStreak.builder()
                        .user(user)
                        .streakDays(0)
                        .streakProtected(false)
                        .streakFreezeCount(1)
                        .build());

        boolean isNewDay;
        String status;
        String message;

        if (streak.getLastActiveDate() == null) {
            // 1. Initial streak activation
            int initialDays = Math.max(1, streak.getStreakDays() != null && streak.getStreakDays() > 0 ? streak.getStreakDays() : 1);
            streak.setStreakDays(initialDays);
            streak.setLastActiveDate(activeDate);
            isNewDay = true;
            status = "STREAK_INITIALIZED";
            message = "Khởi đầu chuỗi học tập: Ngày 1!";
        } else {
            long dayDiff = ChronoUnit.DAYS.between(streak.getLastActiveDate(), activeDate);

            if (dayDiff == 0) {
                // 2. Same-day activity
                isNewDay = false;
                status = "ALREADY_ACTIVE_TODAY";
                int currentDays = Math.max(1, streak.getStreakDays() != null ? streak.getStreakDays() : 1);
                streak.setStreakDays(currentDays);
                message = String.format("Hôm nay bạn đã duy trì chuỗi %d ngày!", currentDays);
            } else if (dayDiff == 1) {
                // 3. Consecutive next day
                int nextDays = (streak.getStreakDays() != null ? streak.getStreakDays() : 0) + 1;
                streak.setStreakDays(nextDays);
                streak.setLastActiveDate(activeDate);
                isNewDay = true;
                status = "STREAK_INCREMENTED";
                message = String.format("Tuyệt vời! Chuỗi học tập đạt %d ngày liên tiếp!", nextDays);
            } else if (dayDiff > 1) {
                // 4. Missed 1 or more days -> check streak freeze protection
                boolean hasProtection = Boolean.TRUE.equals(streak.getStreakProtected())
                        || (streak.getStreakFreezeCount() != null && streak.getStreakFreezeCount() > 0);

                if (hasProtection) {
                    int currentFreeze = streak.getStreakFreezeCount() != null ? streak.getStreakFreezeCount() : 0;
                    int nextFreeze = Boolean.TRUE.equals(streak.getStreakProtected())
                            ? currentFreeze
                            : Math.max(0, currentFreeze - 1);
                    int nextDays = (streak.getStreakDays() != null ? streak.getStreakDays() : 0) + 1;

                    streak.setStreakDays(nextDays);
                    streak.setLastActiveDate(activeDate);
                    streak.setStreakProtected(false);
                    streak.setStreakFreezeCount(nextFreeze);
                    isNewDay = true;
                    status = "STREAK_PRESERVED_BY_FREEZE";
                    message = String.format("Bình Giữ Chuỗi (Streak Freeze) đã bảo vệ chuỗi của bạn! Tiếp tục đạt %d ngày!", nextDays);
                } else {
                    // 5. Unprotected streak break -> restart from day 1
                    streak.setStreakDays(1);
                    streak.setLastActiveDate(activeDate);
                    isNewDay = true;
                    status = "STREAK_RESET";
                    message = "Chuỗi học tập đã được làm mới: Ngày 1! Hãy kiên trì mỗi ngày nhé!";
                }
            } else {
                // Negative dayDiff (clock skew or time-traveled backwards)
                isNewDay = false;
                status = "ALREADY_ACTIVE_TODAY";
                message = String.format("Chuỗi học tập: %d ngày.", Math.max(1, streak.getStreakDays() != null ? streak.getStreakDays() : 1));
            }
        }

        streak = userStreakRepository.save(streak);
        long totalSeals = userSealRepository.countByUserId(user.getId());

        return StreakSyncResponse.builder()
                .streak(toDto(streak, totalSeals))
                .isNewDay(isNewDay)
                .status(status)
                .message(message)
                .build();
    }

    @Override
    @Transactional
    public SealLessonResponse sealLesson(String userEmail, SealLessonRequest request) {
        User user = findUser(userEmail);
        LocalDate today = LocalDate.now();

        // Technical constraint: Prevent duplicate seal records for the same user, reading post, and calendar day
        Optional<UserSeal> existingSealOpt = userSealRepository
                .findByUserIdAndReadingPostIdAndSealedDate(user.getId(), request.getReadingPostId(), today);

        UserSeal seal;
        boolean isNewSeal;
        String mascotMessage;

        if (existingSealOpt.isPresent()) {
            seal = existingSealOpt.get();
            isNewSeal = false;
            mascotMessage = "Bài học này đã được đóng Mộc Son hôm nay! Bạn đang duy trì tiến độ rất tốt!";
        } else {
            seal = UserSeal.builder()
                    .user(user)
                    .readingPostId(request.getReadingPostId())
                    .sealedDate(today)
                    .wordsLearnedCount(request.getWordsLearnedCount() != null ? request.getWordsLearnedCount() : 0)
                    .source(request.getSource() != null ? request.getSource() : "reader")
                    .build();
            seal = userSealRepository.save(seal);
            isNewSeal = true;
            mascotMessage = String.format(
                    "Chúc mừng! Bạn đã hoàn thành bài học và đóng thành công Mộc Son Đỏ di sản cùng %d từ vựng mới!",
                    seal.getWordsLearnedCount()
            );
        }

        // Trigger streak synchronization for the active day
        StreakSyncResponse streakSyncResponse = syncStreak(userEmail, today);

        return SealLessonResponse.builder()
                .seal(toSealDto(seal))
                .isNewSeal(isNewSeal)
                .streak(streakSyncResponse)
                .mascotMessage(mascotMessage)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSealDto> getUserSeals(String userEmail) {
        User user = findUser(userEmail);
        return userSealRepository.findByUserIdOrderBySealedAtDesc(user.getId())
                .stream()
                .map(this::toSealDto)
                .toList();
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private UserStreakDto toDto(UserStreak streak, long totalSeals) {
        return UserStreakDto.builder()
                .streakDays(streak.getStreakDays())
                .lastActiveDate(streak.getLastActiveDate())
                .streakProtected(streak.getStreakProtected())
                .streakFreezeCount(streak.getStreakFreezeCount())
                .updatedAt(streak.getUpdatedAt())
                .totalSeals(totalSeals)
                .build();
    }

    private UserSealDto toSealDto(UserSeal seal) {
        return UserSealDto.builder()
                .id(seal.getId())
                .readingPostId(seal.getReadingPostId())
                .sealedAt(seal.getSealedAt())
                .sealedDate(seal.getSealedDate())
                .wordsLearnedCount(seal.getWordsLearnedCount())
                .source(seal.getSource())
                .build();
    }
}
