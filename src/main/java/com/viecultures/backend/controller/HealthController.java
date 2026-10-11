package com.viecultures.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = "*")
@Tag(name = "0. System Health", description = "Kiểm tra tình trạng hoạt động của dịch vụ")
public class HealthController {

    @Data
    @Builder
    public static class HealthResponse {
        private String status;
        private String message;
        private LocalDateTime timestamp;
    }

    @Operation(summary = "Kiểm tra sức khỏe hệ thống", description = "Trả về trạng thái UP và thời gian hệ thống")
    @GetMapping
    public ResponseEntity<HealthResponse> checkHealth() {
        HealthResponse response = HealthResponse.builder()
                .status("UP")
                .message("VieCultures Backend is running successfully")
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(response);
    }
}
