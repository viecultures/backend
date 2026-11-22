package com.viecultures.backend.modules.gamification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogResponse {

    private String id;
    private String userId;
    private String userEmail;
    private String anonymousSessionId;
    private String eventType;
    private String eventCategory;
    private String path;
    private String payload;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
}
