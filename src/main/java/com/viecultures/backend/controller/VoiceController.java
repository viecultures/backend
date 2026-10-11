package com.viecultures.backend.controller;

import com.viecultures.backend.dto.GenerateVoiceTrackRequest;
import com.viecultures.backend.dto.VoiceGenerateRequest;
import com.viecultures.backend.dto.VoiceGenerateResponse;
import com.viecultures.backend.dto.VoiceModelDto;
import com.viecultures.backend.dto.VoiceTrackResponse;
import com.viecultures.backend.service.GeminiVoiceService;
import com.viecultures.backend.service.VoiceTrackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
@Tag(name = "2. Neural Voice & TTS Models", description = "Dịch vụ chuyển văn bản thành giọng nói đa tốc độ và danh sách model AI Voice")
public class VoiceController {

    private final GeminiVoiceService geminiVoiceService;
    private final VoiceTrackService voiceTrackService;

    @Operation(summary = "Lấy danh sách các model giọng đọc Azure & AI chọn lọc", description = "Trả về danh sách các model (Amanda, Dustin, Jenny, Guy, Sonia, v.v.) để Admin chọn khi tạo bài viết")
    @GetMapping("/models")
    public ResponseEntity<List<VoiceModelDto>> listVoiceModels() {
        return ResponseEntity.ok(voiceTrackService.getAvailableModels());
    }

    @Operation(summary = "Tạo 2 phiên bản tốc độ (Chậm & Nhanh) cho bài viết theo Model chọn", description = "Tự động lấy văn bản tiếng Anh, gọi AI sinh file Chậm (-15%) và Nhanh (+15%) lưu sẵn trên server")
    @PostMapping("/generate-track")
    public ResponseEntity<VoiceTrackResponse> generateVoiceTrack(@RequestBody GenerateVoiceTrackRequest request) {
        log.info("Request to generate track: voiceId={}, titleSlug={}", request.getVoiceId(), request.getTitleSlug());
        try {
            VoiceTrackResponse response = voiceTrackService.generateTrack(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(VoiceTrackResponse.builder()
                    .success(false)
                    .message(iae.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Failed to generate voice track: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(VoiceTrackResponse.builder()
                    .success(false)
                    .message("Lỗi khi sinh giọng đọc bài viết: " + e.getMessage())
                    .build());
        }
    }

    @Operation(summary = "Lấy danh sách giọng đọc Gemini", description = "Trả về danh sách 5 giọng đọc AI chất lượng cao (Puck, Charon, Kore, Fenrir, Aoede)")
    @GetMapping("/voices")
    public ResponseEntity<List<Map<String, String>>> listVoices() {
        List<Map<String, String>> voices = List.of(
                Map.of("id", "Puck", "name", "Puck", "gender", "Male", "desc",
                        "Giọng Nam trầm, tự nhiên, nhịp điệu bài giảng"),
                Map.of("id", "Charon", "name", "Charon", "gender", "Male", "desc",
                        "Giọng Nam trầm ấm, học thuật và điềm đạm"),
                Map.of("id", "Kore", "name", "Kore", "gender", "Female", "desc", "Giọng Nữ nhẹ nhàng, truyền cảm hứng"),
                Map.of("id", "Fenrir", "name", "Fenrir", "gender", "Male", "desc", "Giọng Nam truyền cảm, rõ âm"),
                Map.of("id", "Aoede", "name", "Aoede", "gender", "Female", "desc",
                        "Giọng Nữ sang trọng, phát âm chuẩn mực"));
        return ResponseEntity.ok(voices);
    }

    @Operation(summary = "Sinh file âm thanh AI cho bài đọc (Gemini legacy)", description = "Gọi Google Gemini API sinh file âm thanh WAV và lưu trữ sẵn cho web phát trực tiếp")
    @PostMapping("/generate")
    public ResponseEntity<VoiceGenerateResponse> generateVoice(@RequestBody VoiceGenerateRequest request) {
        log.info("Received request to generate voice for: voice={}, filename={}",
                request.getVoiceName(), request.getOutputFilename());

        try {
            String audioUrl = geminiVoiceService.generatePostAudio(
                    request.getTextContent(),
                    request.getVoiceName(),
                    request.getOutputFilename());

            VoiceGenerateResponse response = VoiceGenerateResponse.builder()
                    .success(true)
                    .audioUrl(audioUrl)
                    .filename(request.getOutputFilename())
                    .voiceName(request.getVoiceName())
                    .message("Audio generated successfully")
                    .timestamp(LocalDateTime.now())
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to generate voice: {}", e.getMessage());
            VoiceGenerateResponse errorResponse = VoiceGenerateResponse.builder()
                    .success(false)
                    .message("Error generating voice: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
