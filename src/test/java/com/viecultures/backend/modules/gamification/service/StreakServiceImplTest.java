package com.viecultures.backend.modules.gamification.service;

import com.viecultures.backend.modules.gamification.service.impl.StreakServiceImpl;

import com.viecultures.backend.modules.gamification.dto.SealLessonRequest;
import com.viecultures.backend.modules.gamification.dto.SealLessonResponse;
import com.viecultures.backend.modules.gamification.dto.StreakSyncResponse;
import com.viecultures.backend.modules.gamification.dto.UserStreakDto;
import com.viecultures.backend.entity.User;
import com.viecultures.backend.entity.UserSeal;
import com.viecultures.backend.entity.UserStreak;
import com.viecultures.backend.repository.UserSealRepository;
import com.viecultures.backend.repository.UserStreakRepository;
import com.viecultures.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreakServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserStreakRepository userStreakRepository;

    @Mock
    private UserSealRepository userSealRepository;

    @InjectMocks
    private StreakServiceImpl streakService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user-uuid-1")
                .email("learner@viecultures.com")
                .name("Nguyen Van A")
                .build();
    }

    @Test
    @DisplayName("getUserStreak returns default streak state when user has no streak record")
    void testGetUserStreakDefaults() {
        when(userRepository.findByEmail("learner@viecultures.com")).thenReturn(Optional.of(testUser));
        when(userSealRepository.countByUserId("user-uuid-1")).thenReturn(3L);
        when(userStreakRepository.findByUserId("user-uuid-1")).thenReturn(Optional.empty());

        UserStreakDto result = streakService.getUserStreak("learner@viecultures.com");

        assertNotNull(result);
        assertEquals(0, result.getStreakDays());
        assertNull(result.getLastActiveDate());
        assertFalse(result.getStreakProtected());
        assertEquals(1, result.getStreakFreezeCount());
        assertEquals(3L, result.getTotalSeals());
    }

    @Test
    @DisplayName("syncStreak: initial activation initializes streak to 1 day")
    void testSyncStreakInitial() {
        LocalDate today = LocalDate.now();
        when(userRepository.findByEmail("learner@viecultures.com")).thenReturn(Optional.of(testUser));
        when(userStreakRepository.findByUserId("user-uuid-1")).thenReturn(Optional.empty());
        when(userStreakRepository.save(any(UserStreak.class))).thenAnswer(i -> i.getArgument(0));
        when(userSealRepository.countByUserId("user-uuid-1")).thenReturn(0L);

        StreakSyncResponse response = streakService.syncStreak("learner@viecultures.com", today);

        assertNotNull(response);
        assertEquals("STREAK_INITIALIZED", response.getStatus());
        assertTrue(response.getIsNewDay());
        assertEquals(1, response.getStreak().getStreakDays());
        assertEquals(today, response.getStreak().getLastActiveDate());
    }

    @Test
    @DisplayName("syncStreak: same day activity does not increment streak")
    void testSyncStreakSameDay() {
        LocalDate today = LocalDate.now();
        UserStreak existing = UserStreak.builder()
                .id("streak-1")
                .user(testUser)
                .streakDays(5)
                .lastActiveDate(today)
                .streakFreezeCount(1)
                .streakProtected(false)
                .build();

        when(userRepository.findByEmail("learner@viecultures.com")).thenReturn(Optional.of(testUser));
        when(userStreakRepository.findByUserId("user-uuid-1")).thenReturn(Optional.of(existing));
        when(userStreakRepository.save(any(UserStreak.class))).thenAnswer(i -> i.getArgument(0));
        when(userSealRepository.countByUserId("user-uuid-1")).thenReturn(2L);

        StreakSyncResponse response = streakService.syncStreak("learner@viecultures.com", today);

        assertNotNull(response);
        assertEquals("ALREADY_ACTIVE_TODAY", response.getStatus());
        assertFalse(response.getIsNewDay());
        assertEquals(5, response.getStreak().getStreakDays());
    }

    @Test
    @DisplayName("syncStreak: consecutive day increments streak by 1")
    void testSyncStreakConsecutiveDay() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate today = LocalDate.now();
        UserStreak existing = UserStreak.builder()
                .id("streak-1")
                .user(testUser)
                .streakDays(4)
                .lastActiveDate(yesterday)
                .streakFreezeCount(1)
                .streakProtected(false)
                .build();

        when(userRepository.findByEmail("learner@viecultures.com")).thenReturn(Optional.of(testUser));
        when(userStreakRepository.findByUserId("user-uuid-1")).thenReturn(Optional.of(existing));
        when(userStreakRepository.save(any(UserStreak.class))).thenAnswer(i -> i.getArgument(0));
        when(userSealRepository.countByUserId("user-uuid-1")).thenReturn(2L);

        StreakSyncResponse response = streakService.syncStreak("learner@viecultures.com", today);

        assertNotNull(response);
        assertEquals("STREAK_INCREMENTED", response.getStatus());
        assertTrue(response.getIsNewDay());
        assertEquals(5, response.getStreak().getStreakDays());
        assertEquals(today, response.getStreak().getLastActiveDate());
    }

    @Test
    @DisplayName("syncStreak: missed 2 days with freeze count preserves streak and consumes 1 freeze")
    void testSyncStreakFreezeProtection() {
        LocalDate twoDaysAgo = LocalDate.now().minusDays(2);
        LocalDate today = LocalDate.now();
        UserStreak existing = UserStreak.builder()
                .id("streak-1")
                .user(testUser)
                .streakDays(10)
                .lastActiveDate(twoDaysAgo)
                .streakFreezeCount(1)
                .streakProtected(false)
                .build();

        when(userRepository.findByEmail("learner@viecultures.com")).thenReturn(Optional.of(testUser));
        when(userStreakRepository.findByUserId("user-uuid-1")).thenReturn(Optional.of(existing));
        when(userStreakRepository.save(any(UserStreak.class))).thenAnswer(i -> i.getArgument(0));
        when(userSealRepository.countByUserId("user-uuid-1")).thenReturn(5L);

        StreakSyncResponse response = streakService.syncStreak("learner@viecultures.com", today);

        assertNotNull(response);
        assertEquals("STREAK_PRESERVED_BY_FREEZE", response.getStatus());
        assertTrue(response.getIsNewDay());
        assertEquals(11, response.getStreak().getStreakDays());
        assertEquals(0, response.getStreak().getStreakFreezeCount());
        assertFalse(response.getStreak().getStreakProtected());
    }

    @Test
    @DisplayName("syncStreak: missed 3 days without freeze protection resets streak to 1")
    void testSyncStreakReset() {
        LocalDate threeDaysAgo = LocalDate.now().minusDays(3);
        LocalDate today = LocalDate.now();
        UserStreak existing = UserStreak.builder()
                .id("streak-1")
                .user(testUser)
                .streakDays(10)
                .lastActiveDate(threeDaysAgo)
                .streakFreezeCount(0)
                .streakProtected(false)
                .build();

        when(userRepository.findByEmail("learner@viecultures.com")).thenReturn(Optional.of(testUser));
        when(userStreakRepository.findByUserId("user-uuid-1")).thenReturn(Optional.of(existing));
        when(userStreakRepository.save(any(UserStreak.class))).thenAnswer(i -> i.getArgument(0));
        when(userSealRepository.countByUserId("user-uuid-1")).thenReturn(5L);

        StreakSyncResponse response = streakService.syncStreak("learner@viecultures.com", today);

        assertNotNull(response);
        assertEquals("STREAK_RESET", response.getStatus());
        assertTrue(response.getIsNewDay());
        assertEquals(1, response.getStreak().getStreakDays());
        assertEquals(today, response.getStreak().getLastActiveDate());
    }

    @Test
    @DisplayName("sealLesson: stamps new seal and activates streak")
    void testSealLessonNew() {
        LocalDate today = LocalDate.now();
        SealLessonRequest request = SealLessonRequest.builder()
                .readingPostId("tet-tradition")
                .wordsLearnedCount(6)
                .source("reader")
                .build();

        when(userRepository.findByEmail("learner@viecultures.com")).thenReturn(Optional.of(testUser));
        when(userSealRepository.findByUserIdAndReadingPostIdAndSealedDate("user-uuid-1", "tet-tradition", today))
                .thenReturn(Optional.empty());
        when(userSealRepository.save(any(UserSeal.class))).thenAnswer(i -> {
            UserSeal s = i.getArgument(0);
            s.setId("seal-gen-123");
            return s;
        });

        // Mock for streak sync
        when(userStreakRepository.findByUserId("user-uuid-1")).thenReturn(Optional.empty());
        when(userStreakRepository.save(any(UserStreak.class))).thenAnswer(i -> i.getArgument(0));
        when(userSealRepository.countByUserId("user-uuid-1")).thenReturn(1L);

        SealLessonResponse response = streakService.sealLesson("learner@viecultures.com", request);

        assertNotNull(response);
        assertTrue(response.getIsNewSeal());
        assertEquals("seal-gen-123", response.getSeal().getId());
        assertEquals("tet-tradition", response.getSeal().getReadingPostId());
        assertEquals(6, response.getSeal().getWordsLearnedCount());
        assertNotNull(response.getStreak());
        verify(userSealRepository, times(1)).save(any(UserSeal.class));
    }

    @Test
    @DisplayName("sealLesson: prevents duplicate seal on same day and returns existing record")
    void testSealLessonDuplicatePrevention() {
        LocalDate today = LocalDate.now();
        SealLessonRequest request = SealLessonRequest.builder()
                .readingPostId("tet-tradition")
                .wordsLearnedCount(6)
                .source("reader")
                .build();

        UserSeal existingSeal = UserSeal.builder()
                .id("seal-existing-456")
                .user(testUser)
                .readingPostId("tet-tradition")
                .sealedDate(today)
                .wordsLearnedCount(6)
                .source("reader")
                .build();

        when(userRepository.findByEmail("learner@viecultures.com")).thenReturn(Optional.of(testUser));
        when(userSealRepository.findByUserIdAndReadingPostIdAndSealedDate("user-uuid-1", "tet-tradition", today))
                .thenReturn(Optional.of(existingSeal));

        // Mock for streak sync
        UserStreak existingStreak = UserStreak.builder()
                .id("streak-1")
                .user(testUser)
                .streakDays(3)
                .lastActiveDate(today)
                .build();
        when(userStreakRepository.findByUserId("user-uuid-1")).thenReturn(Optional.of(existingStreak));
        when(userStreakRepository.save(any(UserStreak.class))).thenAnswer(i -> i.getArgument(0));
        when(userSealRepository.countByUserId("user-uuid-1")).thenReturn(1L);

        SealLessonResponse response = streakService.sealLesson("learner@viecultures.com", request);

        assertNotNull(response);
        assertFalse(response.getIsNewSeal());
        assertEquals("seal-existing-456", response.getSeal().getId());
        // Verify save was NOT called for duplicate
        verify(userSealRepository, never()).save(any(UserSeal.class));
    }
}
