package com.viecultures.backend.controller;

import com.viecultures.backend.dto.ApiResponse;
import com.viecultures.backend.dto.streak.*;
import com.viecultures.backend.service.StreakService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/user/streak", "/api/v1/user/streak"})
@RequiredArgsConstructor
@Tag(name = "5.1 Gamification Streak & Cultural Seal", description = "Quản lý chuỗi ngày học tập (Streak) và nghi thức đóng Mộc Son Đỏ di sản")
@SecurityRequirement(name = "bearerAuth")
public class StreakController {

    private final StreakService streakService;

    @GetMapping
    @Operation(
            summary = "Lấy trạng thái chuỗi học tập và số Mộc Son",
            description = "Trả về số ngày duy trì chuỗi hiện tại, ngày hoạt động gần nhất, và số lượt Bình Giữ Chuỗi (Streak Freeze)."
    )
    public ResponseEntity<ApiResponse<UserStreakDto>> getStreak(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Vui lòng đăng nhập để xem thông tin chuỗi học tập", "AUTH_REQUIRED"));
        }

        UserStreakDto streakDto = streakService.getUserStreak(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(streakDto, "Lấy thông tin chuỗi học tập thành công"));
    }

    @PostMapping("/sync")
    @Operation(
            summary = "Đồng bộ và tính toán chuỗi học tập ngày",
            description = "Đánh giá ngày học liên tiếp theo logic độ lệch lịch (Calendar Day Difference), kích hoạt streak freeze hoặc reset nếu đứt chuỗi."
    )
    public ResponseEntity<ApiResponse<StreakSyncResponse>> syncStreak(
            @RequestBody(required = false) StreakSyncRequest request,
            Authentication authentication
    ) {
        if (!isAuthenticated(authentication)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Vui lòng đăng nhập để đồng bộ chuỗi học tập", "AUTH_REQUIRED"));
        }

        StreakSyncResponse response = streakService.syncStreak(
                authentication.getName(),
                request != null ? request.getTargetDate() : null
        );
        return ResponseEntity.ok(ApiResponse.ok(response, response.getMessage()));
    }

    @PostMapping("/seal")
    @Operation(
            summary = "Nghi thức đóng Mộc Son Đỏ và kích hoạt Streak ngày",
            description = "Lưu con dấu Mộc Son văn hóa khi hoàn thành bài đọc / ôn tập, ngăn ngừa trùng lặp trong cùng ngày và tự động cập nhật chuỗi học."
    )
    public ResponseEntity<ApiResponse<SealLessonResponse>> sealLesson(
            @Valid @RequestBody SealLessonRequest request,
            Authentication authentication
    ) {
        if (!isAuthenticated(authentication)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Vui lòng đăng nhập để lưu Mộc Son văn hóa", "AUTH_REQUIRED"));
        }

        SealLessonResponse response = streakService.sealLesson(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok(response, response.getMascotMessage()));
    }

    @GetMapping("/seals")
    @Operation(
            summary = "Lấy danh sách các Mộc Son đã mở khóa",
            description = "Trả về toàn bộ các mộc son văn hóa người dùng đã hoàn thành theo thứ tự mới nhất."
    )
    public ResponseEntity<ApiResponse<List<UserSealDto>>> getUserSeals(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Vui lòng đăng nhập để xem danh sách Mộc Son", "AUTH_REQUIRED"));
        }

        List<UserSealDto> seals = streakService.getUserSeals(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(seals, "Lấy danh sách Mộc Son thành công"));
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }
}
