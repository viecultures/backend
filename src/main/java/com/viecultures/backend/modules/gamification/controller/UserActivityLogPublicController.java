package com.viecultures.backend.modules.gamification.controller;

import com.viecultures.backend.common.response.ApiResponse;
import com.viecultures.backend.modules.gamification.dto.ActivityLogBatchRequest;
import com.viecultures.backend.entity.User;
import com.viecultures.backend.repository.UserRepository;
import com.viecultures.backend.modules.gamification.service.UserActivityLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping({"/api/analytics/events", "/api/v1/analytics/events"})
@RequiredArgsConstructor
@Tag(name = "7. Telemetry & Analytics", description = "Thu thap nhat ky hanh vi nguoi dung (Public Telemetry Ingest)")
public class UserActivityLogPublicController {

    private final UserActivityLogService userActivityLogService;
    private final UserRepository userRepository;

    @PostMapping("/batch")
    @Operation(
            summary = "Ghi nhan lo su kien hanh vi nguoi dung (Public Ingest)",
            description = "Nhan danh sach cac su kien tu client web/app theo lo de toi uu hoa hieu nang mang va luu tru."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> ingestBatch(
            @Valid @RequestBody ActivityLogBatchRequest request,
            HttpServletRequest servletRequest,
            Authentication authentication
    ) {
        try {
            String ipAddress = extractClientIp(servletRequest);
            String userAgent = servletRequest.getHeader("User-Agent");

            String authUserId = null;
            String authUserEmail = null;

            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                authUserEmail = authentication.getName();
                try {
                    Optional<User> userOpt = userRepository.findByEmail(authUserEmail);
                    if (userOpt.isPresent()) {
                        authUserId = userOpt.get().getId();
                    }
                } catch (Exception ex) {
                    log.error("Failed to query user by email {}: {}", authUserEmail, ex.getMessage());
                }
            }

            userActivityLogService.saveBatch(request, ipAddress, userAgent, authUserId, authUserEmail);

            int eventCount = request.getEvents() != null ? request.getEvents().size() : 0;
            return ResponseEntity.ok(ApiResponse.ok(
                    Map.of("ingestedCount", eventCount),
                    "Nhat ky hanh vi da duoc ghi nhan thanh cong"
            ));
        } catch (Exception e) {
            log.error("Failed to ingest activity logs batch: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Khong the ghi nhan nhat ky hoat dong", "INGESTION_ERROR"));
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor) && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp) && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }
}
