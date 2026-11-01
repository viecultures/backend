package com.viecultures.backend.controller;

import com.viecultures.backend.dto.ApiResponse;
import com.viecultures.backend.dto.analytics.ActivityLogResponse;
import com.viecultures.backend.dto.analytics.ActivityStatsSummaryDto;
import com.viecultures.backend.service.UserActivityLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping({"/api/admin/activity-logs", "/api/v1/admin/activity-logs"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "5.2 Admin Activity Logs", description = "Xem va phan tich nhat ky hanh vi nguoi dung danh cho Quan tri vien")
@SecurityRequirement(name = "bearerAuth")
public class AdminActivityLogController {

    private final UserActivityLogService userActivityLogService;

    @GetMapping
    @Operation(
            summary = "Lay danh sach nhat ky hanh vi nguoi dung phan trang va loc",
            description = "Tra cuu nhat ky theo eventType, eventCategory, userId, tim kiem tu do hoac khoang thoi gian."
    )
    public ResponseEntity<ApiResponse<Page<ActivityLogResponse>>> getLogs(
            @Parameter(description = "Loai su kien (vi du: PAGE_VIEW, ARTICLE_READ, VOCAB_LOOKUP...)")
            @RequestParam(required = false) String eventType,
            @Parameter(description = "Nhom su kien (vi du: READER, VOCABULARY, DISCOVERY...)")
            @RequestParam(required = false) String eventCategory,
            @Parameter(description = "ID nguoi dung can loc")
            @RequestParam(required = false) String userId,
            @Parameter(description = "Tu khoa tim kiem trong path, payload, email, ip")
            @RequestParam(required = false) String search,
            @Parameter(description = "Ngay bat dau (dinh dang yyyy-MM-dd hoac ISO)")
            @RequestParam(required = false) String fromDate,
            @Parameter(description = "Ngay ket thuc (dinh dang yyyy-MM-dd hoac ISO)")
            @RequestParam(required = false) String toDate,
            @Parameter(description = "So thu tu trang (bat dau tu 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Kich thuoc trang (1-100)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Thuoc tinh sap xep (mac dinh: createdAt)")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Chieu sap xep (ASC hoac DESC)")
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        try {
            int safePage = Math.max(0, page);
            int safeSize = Math.min(Math.max(1, size), 100);

            Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
            String safeSortBy = "createdAt".equalsIgnoreCase(sortBy) ? "createdAt" : "createdAt";

            Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(sortDirection, safeSortBy));

            Page<ActivityLogResponse> result = userActivityLogService.getLogs(
                    eventType, eventCategory, userId, search, fromDate, toDate, pageable
            );

            return ResponseEntity.ok(ApiResponse.ok(result, "Lay danh sach nhat ky hanh vi thanh cong"));
        } catch (Exception e) {
            log.error("Error retrieving activity logs: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Khong the tai nhat ky hanh vi", "QUERY_ERROR"));
        }
    }

    @GetMapping("/stats")
    @Operation(
            summary = "Thong ke tong quan nhat ky hanh vi",
            description = "Tong so luot ghi nhan, phan bo theo danh muc, top bai viet duoc xem va top tu vung duoc tra cuu."
    )
    public ResponseEntity<ApiResponse<ActivityStatsSummaryDto>> getStatsSummary() {
        try {
            ActivityStatsSummaryDto stats = userActivityLogService.getStatsSummary();
            return ResponseEntity.ok(ApiResponse.ok(stats, "Lay thong ke nhat ky hanh vi thanh cong"));
        } catch (Exception e) {
            log.error("Error retrieving activity stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Khong the tai thong ke nhat ky hanh vi", "STATS_ERROR"));
        }
    }
}
