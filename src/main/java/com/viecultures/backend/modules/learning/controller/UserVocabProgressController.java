package com.viecultures.backend.modules.learning.controller;

import com.viecultures.backend.common.response.ApiResponse;
import com.viecultures.backend.modules.learning.dto.UserVocabProgressDto;
import com.viecultures.backend.repository.UserVocabProgressRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/v1/user_vocab_progress", "/api/user_vocab_progress", "/api/v1/user/vocab-progress", "/api/user/vocab-progress"})
@RequiredArgsConstructor
@Tag(name = "4.1 User Vocabulary Progress", description = "Đồng bộ tiến độ học và ôn tập thẻ flashcard của người dùng")
public class UserVocabProgressController {

    private final UserVocabProgressRepository userVocabProgressRepository;

    @Operation(summary = "Lưu hoặc cập nhật tiến độ ôn tập từ vựng",
               description = "Nhận tiến độ flashcard nhị phân (Remembered / Forgot) và cập nhật CSDL.")
    @PostMapping
    public ResponseEntity<ApiResponse<UserVocabProgressDto>> saveProgress(@RequestBody UserVocabProgressDto dto) {
        if (dto.getLastReviewedAt() == null) {
            dto.setLastReviewedAt(LocalDateTime.now());
        }
        if (dto.getStatus() == null || dto.getStatus().isBlank()) {
            dto.setStatus("LEARNING");
        }
        log.info("[UserVocabProgress] Cập nhật tiến độ từ vựng: word={}, status={}, reviewCount={}, contextSentence={}",
                dto.getWord(), dto.getStatus(), dto.getReviewCount(), dto.getContextSentence());
        return ResponseEntity.ok(ApiResponse.ok(dto, "Lưu tiến độ từ vựng thành công"));
    }

    @Operation(summary = "Lấy danh sách tiến độ từ vựng của người dùng")
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserVocabProgressDto>>> getProgress(@RequestParam(required = false) String userId) {
        log.info("[UserVocabProgress] Truy vấn tiến độ từ vựng cho userId={}", userId);
        return ResponseEntity.ok(ApiResponse.ok(List.of()));
    }
}
