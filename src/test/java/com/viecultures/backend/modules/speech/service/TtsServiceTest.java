package com.viecultures.backend.modules.speech.service;

import com.viecultures.backend.common.exception.TtsException;
import com.viecultures.backend.modules.speech.dto.TtsRequest;
import com.viecultures.backend.modules.speech.dto.TtsResult;
import com.viecultures.backend.integration.azure.AzureSpeechService;
import com.viecultures.backend.integration.gemini.DynamicApiKeyService;
import com.viecultures.backend.integration.gemini.GeminiVoiceService;
import com.viecultures.backend.modules.admin.service.AiCostTrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TtsServiceTest {

    @Mock
    private AzureSpeechService azureSpeechService;

    @Mock
    private GeminiVoiceService geminiVoiceService;

    @Mock
    private TtsCacheService ttsCacheService;

    @Mock
    private DynamicApiKeyService dynamicApiKeyService;

    @Mock
    private AiCostTrackingService aiCostTrackingService;

    @InjectMocks
    private TtsManagerService ttsManagerService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ttsManagerService, "configuredProvider", "azure");
        ReflectionTestUtils.setField(ttsManagerService, "fallbackEnabled", true);
        lenient().when(dynamicApiKeyService.getActiveTtsProvider()).thenReturn("azure");
        lenient().when(dynamicApiKeyService.getActiveConfigId(anyString(), anyString())).thenReturn(1L);
    }

    @Test
    @DisplayName("Cache Hit: Trả về audio từ cache mà không cần gọi API")
    void testCacheHitReturnsImmediately() throws Exception {
        byte[] cachedBytes = "fake-cached-audio-mp3".getBytes();
        when(ttsCacheService.computeMd5(anyString(), anyString(), anyString())).thenReturn("mockhash123");
        when(ttsCacheService.getFromCache(eq("azure"), eq("mockhash123"))).thenReturn(Optional.of(cachedBytes));

        TtsRequest request = TtsRequest.builder()
                .text("Hello world")
                .voice("en-US-JennyNeural")
                .speed("0%")
                .build();

        TtsResult result = ttsManagerService.synthesize(request);

        assertNotNull(result);
        assertTrue(result.isFromCache());
        assertEquals("azure", result.getProvider());
        assertArrayEquals(cachedBytes, result.getAudioData());

        // Đảm bảo không gọi sang Azure hay Gemini
        verify(azureSpeechService, never()).synthesize(anyString(), anyString(), anyString());
        verify(geminiVoiceService, never()).synthesizeAudioBytes(anyString(), anyString());
    }

    @Test
    @DisplayName("Cache Miss: Gọi Azure Speech và lưu vào cache")
    void testCacheMissCallsAzureAndSavesCache() {
        byte[] azureBytes = "azure-audio-mp3".getBytes();
        when(ttsCacheService.computeMd5(anyString(), anyString(), anyString())).thenReturn("mockhash456");
        when(ttsCacheService.getFromCache(eq("azure"), eq("mockhash456"))).thenReturn(Optional.empty());
        when(azureSpeechService.synthesize(eq("Hello world"), eq("en-US-JennyNeural"), eq("0%"))).thenReturn(azureBytes);

        TtsRequest request = TtsRequest.builder()
                .text("Hello world")
                .voice("en-US-JennyNeural")
                .speed("0%")
                .build();

        TtsResult result = ttsManagerService.synthesize(request);

        assertNotNull(result);
        assertFalse(result.isFromCache());
        assertEquals("azure", result.getProvider());
        assertArrayEquals(azureBytes, result.getAudioData());

        verify(azureSpeechService, times(1)).synthesize("Hello world", "en-US-JennyNeural", "0%");
        verify(ttsCacheService, times(1)).putCache("azure", "mockhash456", azureBytes, ".mp3");
    }

    @Test
    @DisplayName("Backup Fallback: Azure lỗi 401/429 tự động fallback sang Gemini")
    void testAzureFailureFallsBackToGemini() throws Exception {
        byte[] geminiBytes = "gemini-audio-backup".getBytes();
        when(ttsCacheService.computeMd5(anyString(), anyString(), anyString())).thenReturn("mockhash789");
        when(ttsCacheService.getFromCache(eq("azure"), eq("mockhash789"))).thenReturn(Optional.empty());
        when(azureSpeechService.synthesize(anyString(), anyString(), anyString()))
                .thenThrow(new TtsException("AZURE_UNAUTHORIZED", 401, "Invalid key"));
        when(geminiVoiceService.synthesizeAudioBytes(anyString(), anyString())).thenReturn(geminiBytes);

        TtsRequest request = TtsRequest.builder()
                .text("Fallback test")
                .voice("en-US-JennyNeural")
                .speed("0%")
                .build();

        TtsResult result = ttsManagerService.synthesize(request);

        assertNotNull(result);
        assertEquals("gemini-backup", result.getProvider());
        assertArrayEquals(geminiBytes, result.getAudioData());
        verify(geminiVoiceService, times(1)).synthesizeAudioBytes("Fallback test", "en-US-JennyNeural");
        verify(ttsCacheService, times(1)).putCache("gemini", "mockhash789", geminiBytes, ".mp3");
    }

    @Test
    @DisplayName("SSML Builder: Chuẩn hóa SSML XML và escape ký tự đặc biệt")
    void testAzureSsmlBuildingAndEscaping() {
        AzureSpeechService service = new AzureSpeechService(null, null);
        String ssml = service.buildSsml("Fish & chips <special> 'quote' \"double\"", "en-US-JennyNeural", "-15%");

        assertTrue(ssml.contains("&amp;"), "Phải escape & thành &amp;");
        assertTrue(ssml.contains("&lt;special&gt;"), "Phải escape < và >");
        assertTrue(ssml.contains("&apos;quote&apos;"), "Phải escape '");
        assertTrue(ssml.contains("&quot;double&quot;"), "Phải escape \"");
        assertTrue(ssml.contains("rate='-15%'"), "Prosody rate phải chứa -15%");
        assertTrue(ssml.contains("voice name='en-US-JennyNeural'"), "Voice name phải chính xác");
    }

    @Test
    @DisplayName("Cache MD5 Computation: Tính hash nhất quán")
    void testMd5Computation() {
        TtsCacheService cacheService = new TtsCacheService();
        String hash1 = cacheService.computeMd5("Hello", "en-US-JennyNeural", "-15%");
        String hash2 = cacheService.computeMd5("Hello", "en-US-JennyNeural", "-15%");
        String hash3 = cacheService.computeMd5("Hello different", "en-US-JennyNeural", "-15%");

        assertEquals(hash1, hash2, "Cùng text + voice + speed phải ra cùng MD5 hash");
        assertNotEquals(hash1, hash3, "Khác text phải ra khác hash");
        assertEquals(32, hash1.length(), "MD5 hex string phải có độ dài 32 ký tự");
    }
}
