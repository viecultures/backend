package com.viecultures.backend.modules.speech.controller;

import com.viecultures.backend.modules.speech.dto.TtsErrorResponse;
import com.viecultures.backend.modules.speech.dto.TtsRequest;
import com.viecultures.backend.modules.speech.dto.TtsResult;
import com.viecultures.backend.modules.speech.service.TtsManagerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TtsControllerTest {

    @Mock
    private TtsManagerService ttsManagerService;

    @InjectMocks
    private TtsController ttsController;

    @Test
    @DisplayName("Validate: Request body null trả về 400 Bad Request")
    void testNullRequestBody() {
        ResponseEntity<?> response = ttsController.synthesize(null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof TtsErrorResponse);
        TtsErrorResponse err = (TtsErrorResponse) response.getBody();
        assertEquals("VALIDATION_ERROR", err.getError());
    }

    @Test
    @DisplayName("Validate: Text rỗng hoặc khoảng trắng trả về 400 Bad Request")
    void testEmptyText() {
        TtsRequest request = TtsRequest.builder().text("   ").build();
        ResponseEntity<?> response = ttsController.synthesize(request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof TtsErrorResponse);
        TtsErrorResponse err = (TtsErrorResponse) response.getBody();
        assertEquals("VALIDATION_ERROR", err.getError());
        assertTrue(err.getMessage().contains("không được để trống"));
    }

    @Test
    @DisplayName("Validate: Text vượt quá 1.000 ký tự trả về 400 Bad Request")
    void testTextExceeds1000Chars() {
        String longText = "a".repeat(1001);
        TtsRequest request = TtsRequest.builder().text(longText).build();
        ResponseEntity<?> response = ttsController.synthesize(request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof TtsErrorResponse);
        TtsErrorResponse err = (TtsErrorResponse) response.getBody();
        assertEquals("VALIDATION_ERROR", err.getError());
        assertTrue(err.getMessage().contains("1.000 ký tự"));
    }

    @Test
    @DisplayName("Thành công: Trả về HTTP 200 kèm audio/mpeg và byte[]")
    void testSuccessReturnsAudioMpeg() {
        byte[] fakeAudio = new byte[]{1, 2, 3, 4};
        TtsResult result = TtsResult.builder()
                .audioData(fakeAudio)
                .provider("azure")
                .fromCache(true)
                .contentType("audio/mpeg")
                .build();

        when(ttsManagerService.synthesize(any())).thenReturn(result);

        TtsRequest request = TtsRequest.builder().text("Hello world").build();
        ResponseEntity<?> response = ttsController.synthesize(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("audio/mpeg", response.getHeaders().getContentType().toString());
        assertEquals("azure", response.getHeaders().getFirst("X-TTS-Provider"));
        assertEquals("true", response.getHeaders().getFirst("X-TTS-Cached"));
        assertArrayEquals(fakeAudio, (byte[]) response.getBody());
    }
}
