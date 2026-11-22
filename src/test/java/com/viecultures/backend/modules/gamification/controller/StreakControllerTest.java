package com.viecultures.backend.modules.gamification.controller;

import com.viecultures.backend.common.response.ApiResponse;
import com.viecultures.backend.modules.gamification.dto.*;
import com.viecultures.backend.modules.gamification.service.StreakService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreakControllerTest {

    @Mock
    private StreakService streakService;

    @InjectMocks
    private StreakController controller;

    @Test
    @DisplayName("getStreak returns 401 UNAUTHORIZED when authentication is null")
    void testGetStreakUnauthenticated() {
        ResponseEntity<ApiResponse<UserStreakDto>> response = controller.getStreak(null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("AUTH_REQUIRED", response.getBody().getErrorCode());
    }

    @Test
    @DisplayName("getStreak returns 200 OK with streak info when authenticated")
    void testGetStreakAuthenticated() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("cuong@example.com");
        when(auth.getName()).thenReturn("cuong@example.com");

        UserStreakDto dto = UserStreakDto.builder()
                .streakDays(7)
                .lastActiveDate(LocalDate.now())
                .streakProtected(false)
                .streakFreezeCount(1)
                .totalSeals(5L)
                .updatedAt(LocalDateTime.now())
                .build();

        when(streakService.getUserStreak("cuong@example.com")).thenReturn(dto);

        ResponseEntity<ApiResponse<UserStreakDto>> response = controller.getStreak(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(7, response.getBody().getData().getStreakDays());
        assertEquals(5L, response.getBody().getData().getTotalSeals());
    }

    @Test
    @DisplayName("syncStreak returns 401 when authentication is null")
    void testSyncStreakUnauthenticated() {
        ResponseEntity<ApiResponse<StreakSyncResponse>> response = controller.syncStreak(null, null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("syncStreak returns 200 OK and incremented streak when authenticated")
    void testSyncStreakAuthenticated() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("cuong@example.com");
        when(auth.getName()).thenReturn("cuong@example.com");

        StreakSyncResponse syncResponse = StreakSyncResponse.builder()
                .streak(UserStreakDto.builder().streakDays(3).lastActiveDate(LocalDate.now()).build())
                .isNewDay(true)
                .status("STREAK_INCREMENTED")
                .message("Tuyệt vời! Chuỗi học tập đạt 3 ngày liên tiếp!")
                .build();

        when(streakService.syncStreak("cuong@example.com", null)).thenReturn(syncResponse);

        ResponseEntity<ApiResponse<StreakSyncResponse>> response = controller.syncStreak(null, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("STREAK_INCREMENTED", response.getBody().getData().getStatus());
        assertEquals(3, response.getBody().getData().getStreak().getStreakDays());
    }

    @Test
    @DisplayName("sealLesson returns 401 when unauthenticated")
    void testSealLessonUnauthenticated() {
        SealLessonRequest request = SealLessonRequest.builder()
                .readingPostId("tet-holiday")
                .wordsLearnedCount(4)
                .build();

        ResponseEntity<ApiResponse<SealLessonResponse>> response = controller.sealLesson(request, null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("sealLesson returns 200 OK with persisted seal and streak update")
    void testSealLessonAuthenticated() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("cuong@example.com");
        when(auth.getName()).thenReturn("cuong@example.com");

        SealLessonRequest request = SealLessonRequest.builder()
                .readingPostId("tet-holiday")
                .wordsLearnedCount(4)
                .source("flashcards")
                .build();

        UserSealDto sealDto = UserSealDto.builder()
                .id("seal-123")
                .readingPostId("tet-holiday")
                .sealedDate(LocalDate.now())
                .wordsLearnedCount(4)
                .source("flashcards")
                .build();

        StreakSyncResponse streakSync = StreakSyncResponse.builder()
                .streak(UserStreakDto.builder().streakDays(4).build())
                .isNewDay(true)
                .status("STREAK_INCREMENTED")
                .message("Tuyệt vời!")
                .build();

        SealLessonResponse sealResponse = SealLessonResponse.builder()
                .seal(sealDto)
                .isNewSeal(true)
                .streak(streakSync)
                .mascotMessage("Chúc mừng bạn đã đóng Mộc Son!")
                .build();

        when(streakService.sealLesson("cuong@example.com", request)).thenReturn(sealResponse);

        ResponseEntity<ApiResponse<SealLessonResponse>> response = controller.sealLesson(request, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getData().getIsNewSeal());
        assertEquals("seal-123", response.getBody().getData().getSeal().getId());
    }

    @Test
    @DisplayName("getUserSeals returns list of unlocked seals")
    void testGetUserSeals() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("cuong@example.com");
        when(auth.getName()).thenReturn("cuong@example.com");

        UserSealDto seal1 = UserSealDto.builder().id("seal-1").readingPostId("post-1").build();
        when(streakService.getUserSeals("cuong@example.com")).thenReturn(List.of(seal1));

        ResponseEntity<ApiResponse<List<UserSealDto>>> response = controller.getUserSeals(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getData().size());
    }
}
