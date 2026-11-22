package com.viecultures.backend.modules.learning.controller;

import com.viecultures.backend.common.response.ApiResponse;
import com.viecultures.backend.modules.learning.dto.CreateReflectionRequest;
import com.viecultures.backend.modules.learning.dto.ReactionResponse;
import com.viecultures.backend.modules.learning.dto.UserReflectionDto;
import com.viecultures.backend.modules.learning.service.UserReflectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserReflectionControllerTest {

    @Mock
    private UserReflectionService userReflectionService;

    @InjectMocks
    private UserReflectionController controller;

    @Test
    @DisplayName("getReflections returns 401 when authentication is null")
    void testGetReflectionsUnauthenticated() {
        ResponseEntity<ApiResponse<List<UserReflectionDto>>> response = controller.getReflections("post-123", null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("AUTH_REQUIRED", response.getBody().getErrorCode());
    }

    @Test
    @DisplayName("getReflections returns 200 and data when authenticated")
    void testGetReflectionsAuthenticated() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("user@example.com");
        when(auth.getName()).thenReturn("user@example.com");

        UserReflectionDto dto = UserReflectionDto.builder()
                .id("ref-1")
                .postId("post-123")
                .content("Bánh Chưng reflects ancestral veneration and deep familial unity.")
                .reactionsCount(5)
                .userLiked(false)
                .status("PUBLISHED")
                .createdAt(LocalDateTime.now())
                .build();

        when(userReflectionService.getReflectionsByPostId("post-123", "user@example.com"))
                .thenReturn(List.of(dto));

        ResponseEntity<ApiResponse<List<UserReflectionDto>>> response = controller.getReflections("post-123", auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("ref-1", response.getBody().getData().get(0).getId());
    }

    @Test
    @DisplayName("createReflection returns 401 when unauthenticated")
    void testCreateReflectionUnauthenticated() {
        CreateReflectionRequest request = CreateReflectionRequest.builder()
                .postId("post-123")
                .content("Valid content over 10 chars")
                .build();

        ResponseEntity<ApiResponse<UserReflectionDto>> response = controller.createReflection(request, null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("createReflection returns 201 when authenticated")
    void testCreateReflectionAuthenticated() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("user@example.com");
        when(auth.getName()).thenReturn("user@example.com");

        CreateReflectionRequest request = CreateReflectionRequest.builder()
                .postId("post-123")
                .content("Valid content over 10 chars")
                .usedVocabIds(List.of("vocab-1"))
                .build();

        UserReflectionDto created = UserReflectionDto.builder()
                .id("ref-new")
                .postId("post-123")
                .content(request.getContent())
                .status("PUBLISHED")
                .build();

        when(userReflectionService.createReflection(eq(request), eq("user@example.com")))
                .thenReturn(created);

        ResponseEntity<ApiResponse<UserReflectionDto>> response = controller.createReflection(request, auth);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("ref-new", response.getBody().getData().getId());
    }

    @Test
    @DisplayName("toggleReaction returns updated reaction state")
    void testToggleReaction() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("user@example.com");
        when(auth.getName()).thenReturn("user@example.com");

        ReactionResponse reactionResp = ReactionResponse.builder()
                .reflectionId("ref-1")
                .reactionsCount(6)
                .userLiked(true)
                .build();

        when(userReflectionService.toggleReaction("ref-1", "user@example.com"))
                .thenReturn(reactionResp);

        ResponseEntity<ApiResponse<ReactionResponse>> response = controller.toggleReaction("ref-1", auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getData().isUserLiked());
        assertEquals(6, response.getBody().getData().getReactionsCount());
    }

    @Test
    @DisplayName("reportReflection flags reflection and returns 200")
    void testReportReflection() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("user@example.com");
        when(auth.getName()).thenReturn("user@example.com");

        ResponseEntity<ApiResponse<Void>> response = controller.reportReflection("ref-1", auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userReflectionService).reportReflection("ref-1", "user@example.com");
    }
}
