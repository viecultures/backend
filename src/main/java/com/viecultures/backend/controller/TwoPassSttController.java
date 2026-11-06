package com.viecultures.backend.controller;

import com.viecultures.backend.dto.ApiResponse;
import com.viecultures.backend.dto.TwoPassSttResponse;
import com.viecultures.backend.service.TwoPassSttService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller cung cấp endpoint nhận dạng giọng nói hai giai đoạn (Two-Pass STT).
 */
@Slf4j
@RestController
@RequestMapping({"/api/v1/voice", "/api/voice"})
@RequiredArgsConstructor
@Tag(name = "2. Neural Voice & Two-Pass STT", description = "Dịch vụ nhận dạng giọng nói hai giai đoạn (Two-Pass STT) kết hợp sửa lỗi phát âm và văn phạm")
public class TwoPassSttController {

    private final TwoPassSttService twoPassSttService;

    @Operation(
            summary = "Nhận dạng giọng nói Two-Pass STT",
            description = "Giai đoạn 1 (Acoustic Transcription) nhận dạng âm thanh thô verbatim. Giai đoạn 2 (Semantic Refinement) chuẩn hóa văn bản, loại bỏ từ đệm và sửa lỗi ngữ cảnh khi smartCleanup = true."
    )
    @PostMapping(value = "/transcribe-two-pass", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TwoPassSttResponse>> transcribeTwoPass(
            @Parameter(
                    description = "Tệp âm thanh cần nhận dạng (định dạng WAV hoặc WebM)",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string", format = "binary"))
            )
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Chế độ làm sạch thông minh: true (hội thoại thông minh - lọc từ đệm), false (luyện phát âm - verbatim 100%)")
            @RequestParam(value = "smartCleanup", defaultValue = "true", required = false) boolean smartCleanup,
            @Parameter(description = "Tùy chọn chế độ nhận dạng: 'smart' hoặc 'verbatim'")
            @RequestParam(value = "mode", required = false) String mode) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Tập tin âm thanh không được để trống", "EMPTY_FILE")
            );
        }

        boolean effectiveSmartCleanup = smartCleanup;
        if (mode != null && !mode.isBlank()) {
            String trimmedMode = mode.trim();
            if ("verbatim".equalsIgnoreCase(trimmedMode)) {
                effectiveSmartCleanup = false;
            } else if ("smart".equalsIgnoreCase(trimmedMode)) {
                effectiveSmartCleanup = true;
            }
        }

        try {
            TwoPassSttResponse response = twoPassSttService.transcribe(file, effectiveSmartCleanup);
            return ResponseEntity.ok(ApiResponse.ok(response, "Nhận dạng giọng nói thành công"));
        } catch (IllegalArgumentException ex) {
            log.warn("Dữ liệu đầu vào không hợp lệ: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(ex.getMessage(), "INVALID_INPUT")
            );
        } catch (Exception ex) {
            log.error("Lỗi trong quá trình xử lý Two-Pass STT: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error("Không thể xử lý âm thanh: " + ex.getMessage(), "STT_PROCESSING_FAILED")
            );
        }
    }
}
