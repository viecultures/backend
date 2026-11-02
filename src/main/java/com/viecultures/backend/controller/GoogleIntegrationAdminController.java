package com.viecultures.backend.controller;

import com.viecultures.backend.dto.ApiResponse;
import com.viecultures.backend.dto.google.Ga4ReportResponse;
import com.viecultures.backend.dto.google.GscReportResponse;
import com.viecultures.backend.service.GoogleIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/google")
@RequiredArgsConstructor
@Tag(name = "Admin Google Analytics & Search Console", description = "API kết nối trực tiếp với Google Analytics 4 và Google Search Console cho quản trị viên")
public class GoogleIntegrationAdminController {

    private final GoogleIntegrationService googleIntegrationService;

    @Operation(
            summary = "Lấy báo cáo thống kê Google Analytics 4 (GA4)",
            description = "Trả về các chỉ số activeUsers, sessions, screenPageViews, averageSessionDuration, timeline theo ngày, top trang truy cập và nguồn lưu lượng (traffic sources)."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy dữ liệu GA4 thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Lỗi khi xử lý dữ liệu GA4")
    })
    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<Ga4ReportResponse>> getAnalytics(
            @Parameter(description = "Khoảng thời gian (ví dụ 7d, 14d, 30d, 90d)", example = "30d")
            @RequestParam(name = "range", defaultValue = "30d") String range
    ) {
        log.info("REST request to fetch GA4 report with range: {}", range);
        try {
            Ga4ReportResponse response = googleIntegrationService.getGa4Report(range);
            return ResponseEntity.ok(ApiResponse.ok(response, "Lấy dữ liệu Google Analytics 4 thành công"));
        } catch (Exception e) {
            log.error("Error retrieving GA4 data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Không thể lấy dữ liệu Google Analytics 4: " + e.getMessage(), "GA4_ERROR"));
        }
    }

    @Operation(
            summary = "Lấy báo cáo thống kê Google Search Console (GSC)",
            description = "Trả về các chỉ số clicks, impressions, CTR, vị trí trung bình (position), timeline hiệu suất theo ngày, từ khóa tìm kiếm hàng đầu (top queries), trang phổ biến và thiết bị."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy dữ liệu GSC thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Lỗi khi xử lý dữ liệu GSC")
    })
    @GetMapping("/search-console")
    public ResponseEntity<ApiResponse<GscReportResponse>> getSearchConsole(
            @Parameter(description = "Khoảng thời gian (ví dụ 7d, 28d, 90d)", example = "28d")
            @RequestParam(name = "range", defaultValue = "28d") String range
    ) {
        log.info("REST request to fetch Google Search Console report with range: {}", range);
        try {
            GscReportResponse response = googleIntegrationService.getGscReport(range);
            return ResponseEntity.ok(ApiResponse.ok(response, "Lấy dữ liệu Google Search Console thành công"));
        } catch (Exception e) {
            log.error("Error retrieving GSC data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Không thể lấy dữ liệu Google Search Console: " + e.getMessage(), "GSC_ERROR"));
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("Unhandled exception in GoogleIntegrationAdminController: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Lỗi máy chủ nội bộ: " + ex.getMessage(), "INTERNAL_SERVER_ERROR"));
    }
}
