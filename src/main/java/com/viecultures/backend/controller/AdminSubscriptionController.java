package com.viecultures.backend.controller;

import com.viecultures.backend.dto.ApiResponse;
import com.viecultures.backend.annotation.AuditLog;
import com.viecultures.backend.dto.admin.*;
import com.viecultures.backend.service.AdminSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/subscriptions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "7. Admin Subscriptions", description = "Quản lý gói cước đăng ký và phê duyệt giao dịch chuyển khoản dành cho Quản trị viên")
public class AdminSubscriptionController {

    private final AdminSubscriptionService subscriptionService;

    @Operation(summary = "Lấy các chỉ số KPI vận hành & doanh thu gói cước")
    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<SubscriptionMetricsDto>> getMetrics() {
        SubscriptionMetricsDto metrics = subscriptionService.getMetrics();
        return ResponseEntity.ok(ApiResponse.ok(metrics, "Lấy chỉ số KPI gói đăng ký thành công"));
    }

    @Operation(summary = "Lấy danh sách nhật ký giao dịch chuyển khoản có lọc theo trạng thái và tìm kiếm")
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<SubscriptionTransactionDto>>> getTransactions(
            @Parameter(description = "Lọc theo trạng thái: ALL, PENDING, APPROVED, REJECTED")
            @RequestParam(required = false) String status,
            @Parameter(description = "Từ khóa tìm kiếm theo email, họ tên, mã giao dịch, nội dung CK")
            @RequestParam(required = false) String query) {
        List<SubscriptionTransactionDto> list = subscriptionService.getTransactions(status, query);
        return ResponseEntity.ok(ApiResponse.ok(list, "Lấy danh sách giao dịch thành công"));
    }

    @Operation(summary = "Duyệt kích hoạt đơn giao dịch chuyển khoản")
    @AuditLog(action = "APPROVE_TRANSACTION", entity = "SUBSCRIPTION", description = "Phê duyệt đơn giao dịch chuyển khoản")
    @PostMapping("/transactions/{id}/approve")
    public ResponseEntity<ApiResponse<SubscriptionTransactionDto>> approveTransaction(
            @PathVariable String id,
            @RequestBody(required = false) ApproveTransactionRequest request) {
        SubscriptionTransactionDto approved = subscriptionService.approveTransaction(id, request);
        return ResponseEntity.ok(ApiResponse.ok(approved, "Duyệt giao dịch thành công"));
    }

    @Operation(summary = "Từ chối đơn giao dịch chuyển khoản kèm lý do")
    @AuditLog(action = "REJECT_TRANSACTION", entity = "SUBSCRIPTION", description = "Từ chối đơn giao dịch chuyển khoản")
    @PostMapping("/transactions/{id}/reject")
    public ResponseEntity<ApiResponse<SubscriptionTransactionDto>> rejectTransaction(
            @PathVariable String id,
            @Valid @RequestBody RejectTransactionRequest request) {
        SubscriptionTransactionDto rejected = subscriptionService.rejectTransaction(id, request);
        return ResponseEntity.ok(ApiResponse.ok(rejected, "Đã từ chối giao dịch thành công"));
    }

    @Operation(summary = "Lấy danh sách tất cả các gói cước đăng ký")
    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<SubscriptionPlanDto>>> getPlans() {
        List<SubscriptionPlanDto> plans = subscriptionService.getPlans();
        return ResponseEntity.ok(ApiResponse.ok(plans, "Lấy danh sách gói cước thành công"));
    }

    @Operation(summary = "Tạo mới hoặc cập nhật gói cước đăng ký")
    @AuditLog(action = "SAVE_PLAN", entity = "SUBSCRIPTION", description = "Tạo mới hoặc cập nhật thông tin gói cước đăng ký")
    @PostMapping("/plans")
    public ResponseEntity<ApiResponse<SubscriptionPlanDto>> savePlan(
            @RequestBody SubscriptionPlanDto planDto) {
        SubscriptionPlanDto saved = subscriptionService.savePlan(planDto);
        return ResponseEntity.ok(ApiResponse.ok(saved, "Lưu gói cước thành công"));
    }

    @Operation(summary = "Bật hoặc tắt trạng thái kinh doanh của gói cước")
    @AuditLog(action = "TOGGLE_PLAN_STATUS", entity = "SUBSCRIPTION", description = "Chuyển trạng thái kinh doanh của gói cước")
    @PatchMapping("/plans/{id}/toggle-status")
    public ResponseEntity<ApiResponse<SubscriptionPlanDto>> togglePlanStatus(
            @PathVariable String id) {
        SubscriptionPlanDto updated = subscriptionService.togglePlanStatus(id);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Chuyển trạng thái gói cước thành công"));
    }
}
