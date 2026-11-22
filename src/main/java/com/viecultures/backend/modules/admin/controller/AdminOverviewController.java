package com.viecultures.backend.modules.admin.controller;

import com.viecultures.backend.common.response.ApiResponse;
import com.viecultures.backend.modules.admin.dto.AdminOverviewStatsDto;
import com.viecultures.backend.modules.admin.service.AdminOverviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/overview")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Overview", description = "API Thống kê tổng quan và các chỉ số vận hành CMS cho Quản trị viên")
public class AdminOverviewController {

    private final AdminOverviewService adminOverviewService;

    @Operation(
            summary = "Lấy dữ liệu thống kê tổng quan hệ thống CMS",
            description = "Trả về tổng số tài khoản người dùng, học viên hoạt động trong tuần, tổng số bài đọc, lượt đọc, tỷ lệ trung bình, độ phủ CEFR và độ phủ vùng miền."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy dữ liệu thống kê thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền hạn Quản trị viên (ROLE_ADMIN)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Lỗi máy chủ nội bộ khi tính toán thống kê")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<AdminOverviewStatsDto>> getOverview() {
        log.info("REST request to fetch admin overview statistics");
        try {
            AdminOverviewStatsDto stats = adminOverviewService.getOverviewStats();
            return ResponseEntity.ok(ApiResponse.ok(stats));
        } catch (Exception e) {
            log.error("Error handling getOverview request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve admin overview statistics: " + e.getMessage(), "INTERNAL_SERVER_ERROR"));
        }
    }
}
