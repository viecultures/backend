package com.viecultures.backend.controller;

import com.viecultures.backend.dto.ApiResponse;
import com.viecultures.backend.dto.reflection.CreateReflectionRequest;
import com.viecultures.backend.dto.reflection.ReactionResponse;
import com.viecultures.backend.dto.reflection.UserReflectionDto;
import com.viecultures.backend.service.UserReflectionService;
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
@RequestMapping({"/api/reflections", "/api/v1/reflections"})
@RequiredArgsConstructor
@Tag(name = "6.1 Community Reflections (UGC)", description = "Quản lý cảm nghĩ người học, tương tác thả tim và kiểm duyệt thụ động không phán xét")
@SecurityRequirement(name = "bearerAuth")
public class UserReflectionController {

    private final UserReflectionService userReflectionService;

    @GetMapping
    @Operation(
            summary = "Lấy danh sách cảm nghĩ cộng đồng theo bài đọc",
            description = "Trả về danh sách reflections đã duyệt hoặc thuộc sở hữu của người dùng hiện tại."
    )
    public ResponseEntity<ApiResponse<List<UserReflectionDto>>> getReflections(
            @RequestParam String postId,
            Authentication authentication
    ) {
        if (!isAuthenticated(authentication)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Vui lòng đăng nhập để xem cảm nghĩ cộng đồng", "AUTH_REQUIRED"));
        }

        List<UserReflectionDto> reflections = userReflectionService.getReflectionsByPostId(postId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(reflections, "Lấy danh sách cảm nghĩ thành công"));
    }

    @PostMapping
    @Operation(
            summary = "Tạo cảm nghĩ cộng đồng mới",
            description = "Ghi lại bài viết cảm nghĩ theo chính sách Không phán xét; tự động lọc nội dung nhạy cảm."
    )
    public ResponseEntity<ApiResponse<UserReflectionDto>> createReflection(
            @Valid @RequestBody CreateReflectionRequest request,
            Authentication authentication
    ) {
        if (!isAuthenticated(authentication)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Vui lòng đăng nhập để chia sẻ cảm nghĩ", "AUTH_REQUIRED"));
        }

        UserReflectionDto created = userReflectionService.createReflection(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Chia sẻ cảm nghĩ thành công"));
    }

    @PostMapping("/{id}/react")
    @Operation(
            summary = "Thả tim / Bỏ thả tim cảm nghĩ (Idempotent toggle)",
            description = "Bật/tắt tương tác thả tim động viên cho bài cảm nghĩ qua bảng user_reflection_reactions."
    )
    public ResponseEntity<ApiResponse<ReactionResponse>> toggleReaction(
            @PathVariable String id,
            Authentication authentication
    ) {
        if (!isAuthenticated(authentication)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Vui lòng đăng nhập để tương tác thả tim", "AUTH_REQUIRED"));
        }

        ReactionResponse reactionResponse = userReflectionService.toggleReaction(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(reactionResponse, "Cập nhật tương tác thành công"));
    }

    @PostMapping("/{id}/report")
    @Operation(
            summary = "Báo cáo cảm nghĩ không phù hợp",
            description = "Gắn cờ kiểm duyệt thụ động để quản trị viên đối soát."
    )
    public ResponseEntity<ApiResponse<Void>> reportReflection(
            @PathVariable String id,
            Authentication authentication
    ) {
        if (!isAuthenticated(authentication)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Vui lòng đăng nhập để báo cáo nội dung", "AUTH_REQUIRED"));
        }

        userReflectionService.reportReflection(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Báo cáo cảm nghĩ thành công. Nội dung đã được chuyển vào hàng đợi kiểm duyệt."));
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }
}
