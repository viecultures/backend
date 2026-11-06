package com.viecultures.backend.controller;

import com.viecultures.backend.dto.ApiResponse;
import com.viecultures.backend.dto.admin.SubscriptionPlanDto;
import com.viecultures.backend.service.AdminSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/v1/subscriptions", "/api/subscriptions"})
@RequiredArgsConstructor
@Tag(name = "Subscription Public", description = "Các API công khai liên quan đến gói cước đăng ký hội viên")
public class SubscriptionPublicController {

    private final AdminSubscriptionService subscriptionService;

    @Operation(summary = "Lấy danh sách các gói cước đang hoạt động dành cho người dùng và khách truy cập")
    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<SubscriptionPlanDto>>> getActivePlans() {
        List<SubscriptionPlanDto> plans = subscriptionService.getActivePlans();
        return ResponseEntity.ok(ApiResponse.ok(plans, "Lấy danh sách gói cước hoạt động thành công"));
    }
}
