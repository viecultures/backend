package com.viecultures.backend.controller;

import com.viecultures.backend.dto.ApiResponse;
import com.viecultures.backend.dto.admin.AdminUserSummaryDto;
import com.viecultures.backend.dto.admin.UpdateUserRoleRequest;
import com.viecultures.backend.dto.admin.UpdateUserStatusRequest;
import com.viecultures.backend.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
               description = "Tìm kiếm theo tên/email, lọc theo vai trò (role) và trạng thái (enabled) kèm phân trang.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminUserSummaryDto>>> getUsers(
            @Parameter(description = "Từ khóa tìm kiếm theo tên hoặc email")
            @RequestParam(required = false) String query,
            @Parameter(description = "Lọc theo vai trò (ROLE_USER, ROLE_ADMIN)")
            @RequestParam(required = false) String role,
            @Parameter(description = "Lọc theo trạng thái kích hoạt (true/false)")
            @RequestParam(required = false) Boolean enabled,
            @Parameter(description = "Số trang (bắt đầu từ 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Kích thước mỗi trang (1-100)")
            @RequestParam(defaultValue = "10") int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminUserSummaryDto> users = adminUserService.getUsers(query, role, enabled, pageable);
        return ResponseEntity.ok(ApiResponse.ok(users));
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

    @Operation(summary = "Cập nhật trạng thái người dùng (Kích hoạt / Khóa)",
               description = "Bật hoặc tắt trạng thái hoạt động của tài khoản người dùng.")
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Admin user management bad argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), "INVALID_ARGUMENT"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        log.warn("Admin user management action not allowed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), "ACTION_NOT_ALLOWED"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        log.error("Admin user management error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), "OPERATION_FAILED"));
    }
}
