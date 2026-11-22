package com.viecultures.backend.modules.speech.controller;

import com.viecultures.backend.modules.speech.dto.TtsErrorResponse;
import com.viecultures.backend.modules.speech.dto.TtsRequest;
import com.viecultures.backend.modules.speech.dto.TtsResult;
import com.viecultures.backend.common.exception.TtsException;
import com.viecultures.backend.modules.speech.service.TtsManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/tts")
@RequiredArgsConstructor
@Tag(name = "3. Text To Speech (TTS)", description = "Dịch vụ phát âm tiếng Anh chuẩn xác hỗ trợ Azure Speech REST API & Google Gemini AI với bộ nhớ đệm (Audio Caching)")
public class TtsController {

    private final TtsManagerService ttsManagerService;

    @Operation(
            summary = "Chuyển văn bản thành giọng nói (TTS Stream)",
            description = "Nhận từ vựng hoặc văn bản tiếng Anh (tối đa 1.000 ký tự), tự động kiểm tra cache MD5 để tiết kiệm hạn mức 500k ký tự Azure, sau đó gọi Azure Speech hoặc Gemini AI và trả về stream âm thanh MP3."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thành công - Trả về luồng âm thanh định dạng audio/mpeg",
                    content = @Content(mediaType = "audio/mpeg")),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ (Văn bản rỗng hoặc vượt quá 1.000 ký tự)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TtsErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Chưa cấu hình hoặc sai khóa bí mật Azure Speech API Key",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TtsErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Vượt quá hạn mức request hoặc gói dịch vụ Free F0",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TtsErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ hoặc kết nối dịch vụ TTS",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TtsErrorResponse.class)))
    })
    @PostMapping(produces = {"audio/mpeg", "application/json"})
    public ResponseEntity<?> synthesize(@RequestBody(required = false) TtsRequest request) {
        if (request == null) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dữ liệu yêu cầu (Request body) không được để trống");
        }

        String text = request.getText();
        if (text == null || text.trim().isEmpty()) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Nội dung văn bản (text) không được để trống");
        }

        if (text.length() > 1000) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Nội dung văn bản không được vượt quá 1.000 ký tự mỗi request (Độ dài hiện tại: " + text.length() + ")");
        }

        try {
            TtsResult result = ttsManagerService.synthesize(request);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(result.getContentType()))
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(result.getAudioData().length))
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .header("X-TTS-Provider", result.getProvider())
                    .header("X-TTS-Cached", String.valueOf(result.isFromCache()))
                    .body(result.getAudioData());

        } catch (TtsException te) {
            log.error("TTS Business Error [{}]: {}", te.getErrorCode(), te.getMessage());
            HttpStatus status = HttpStatus.resolve(te.getStatus());
            if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
            return buildErrorResponse(status, te.getErrorCode(), te.getMessage());
        } catch (Exception ex) {
            log.error("Unhandled TTS Server Error: {}", ex.getMessage(), ex);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "TTS_INTERNAL_ERROR",
                    "Lỗi hệ thống khi chuyển đổi giọng nói: " + ex.getMessage());
        }
    }

    private ResponseEntity<TtsErrorResponse> buildErrorResponse(HttpStatus status, String errorCode, String message) {
        TtsErrorResponse response = TtsErrorResponse.builder()
                .success(false)
                .error(errorCode)
                .message(message)
                .status(status.value())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
}
