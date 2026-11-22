package com.viecultures.backend.modules.admin.controller;

import com.viecultures.backend.common.annotation.AuditLog;
import com.viecultures.backend.common.response.ApiResponse;
import com.viecultures.backend.modules.admin.dto.AdminUserSummaryDto;
import com.viecultures.backend.modules.admin.dto.LearnerDetailDto;
import com.viecultures.backend.modules.admin.dto.UpdateUserRoleRequest;
import com.viecultures.backend.modules.admin.dto.UpdateUserStatusRequest;
import com.viecultures.backend.entity.UserActivityLog;
import com.viecultures.backend.modules.admin.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "5. Admin User Management", description = "Quản lý người dùng, phân quyền và trạng thái tài khoản dành cho Quản trị viên")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "Lấy danh sách người dùng phân trang & tìm kiếm",
               description = "Tìm kiếm theo tên/email, lọc theo vai trò (role), trạng thái (enabled) và phân nhóm marketing (filterGroup) kèm phân trang.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminUserSummaryDto>>> getUsers(
            @Parameter(description = "Từ khóa tìm kiếm theo tên hoặc email")
            @RequestParam(required = false) String query,
            @Parameter(description = "Lọc theo vai trò (ROLE_USER, ROLE_ADMIN)")
            @RequestParam(required = false) String role,
            @Parameter(description = "Lọc theo trạng thái kích hoạt (true/false)")
            @RequestParam(required = false) Boolean enabled,
            @Parameter(description = "Phân nhóm marketing (all, recent_7d, inactive, active, admin)")
            @RequestParam(required = false) String filterGroup,
            @Parameter(description = "Số trang (bắt đầu từ 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Kích thước mỗi trang (1-100)")
            @RequestParam(defaultValue = "10") int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminUserSummaryDto> users = adminUserService.getUsers(query, role, enabled, filterGroup, pageable);
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @Operation(summary = "Xuất danh sách người dùng ra định dạng CSV",
               description = "Xuất dữ liệu người dùng ra tệp tin CSV mã hóa UTF-8 kèm BOM tương thích Microsoft Excel tiếng Việt.")
    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportUsersCsv(
            @Parameter(description = "Từ khóa tìm kiếm theo tên hoặc email")
            @RequestParam(required = false) String query,
            @Parameter(description = "Lọc theo vai trò (ROLE_USER, ROLE_ADMIN)")
            @RequestParam(required = false) String role,
            @Parameter(description = "Lọc theo trạng thái kích hoạt (true/false)")
            @RequestParam(required = false) Boolean enabled,
            @Parameter(description = "Phân nhóm marketing (all, recent_7d, inactive, active, admin)")
            @RequestParam(required = false) String filterGroup) {
        byte[] csvBytes = adminUserService.exportUsersCsv(query, role, enabled, filterGroup);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"viecultures_users.csv\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(csvBytes);
    }

    @Operation(summary = "Xem thông tin chi tiết một người dùng",
               description = "Lấy hồ sơ tóm tắt và thống kê học tập của người dùng theo ID.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminUserSummaryDto>> getUserById(
            @Parameter(description = "ID người dùng")
            @PathVariable String id) {
        AdminUserSummaryDto user = adminUserService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @Operation(summary = "Xem hồ sơ học viên chi tiết 360 độ (Learner Observability)",
               description = "Truy vấn hồ sơ người dùng, gói đăng ký, lịch sử thanh toán, thống kê chuỗi học tập, từ vựng và bài đọc đã hoàn thành.")
    @GetMapping("/{id}/details")
    public ResponseEntity<ApiResponse<LearnerDetailDto>> getLearnerDetails(
            @Parameter(description = "ID người dùng")
            @PathVariable String id) {
        LearnerDetailDto details = adminUserService.getLearnerDetails(id);
        return ResponseEntity.ok(ApiResponse.ok(details));
    }

    @Operation(summary = "Xem nhật ký hoạt động của học viên",
               description = "Lấy danh sách nhật ký sự kiện, thao tác tra cứu, học tập phân trang của học viên theo ID.")
    @GetMapping("/{id}/activity-logs")
    public ResponseEntity<ApiResponse<Page<UserActivityLog>>> getLearnerActivityLogs(
            @Parameter(description = "ID người dùng")
            @PathVariable String id,
            @Parameter(description = "Số trang (bắt đầu từ 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Kích thước mỗi trang (1-100)")
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserActivityLog> logs = adminUserService.getLearnerActivityLogs(id, pageable);
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }

    @Operation(summary = "Cập nhật trạng thái người dùng (Kích hoạt / Khóa)",
               description = "Bật hoặc tắt trạng thái hoạt động của tài khoản người dùng.")
    @AuditLog(action = "UPDATE_USER_STATUS", entity = "USER", description = "Cập nhật trạng thái kích hoạt / khóa tài khoản người dùng")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AdminUserSummaryDto>> updateUserStatus(
            @Parameter(description = "ID người dùng")
            @PathVariable String id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        String adminEmail = currentUser != null ? currentUser.getUsername() : null;
        AdminUserSummaryDto updated = adminUserService.updateUserStatus(id, request.isEnabled(), adminEmail);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Cập nhật trạng thái người dùng thành công"));
    }

    @Operation(summary = "Cập nhật vai trò / phân quyền người dùng",
               description = "Thay đổi vai trò người dùng (ROLE_USER, ROLE_ADMIN).")
    @AuditLog(action = "UPDATE_USER_ROLE", entity = "USER", description = "Cập nhật vai trò / phân quyền người dùng")
    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<AdminUserSummaryDto>> updateUserRole(
            @Parameter(description = "ID người dùng")
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        String adminEmail = currentUser != null ? currentUser.getUsername() : null;
        AdminUserSummaryDto updated = adminUserService.updateUserRole(id, request.getRole(), adminEmail);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Cập nhật vai trò người dùng thành công"));
    }
}
