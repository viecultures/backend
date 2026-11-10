package com.viecultures.backend.dto.admin;

import com.viecultures.backend.entity.CefrLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserSummaryDto {
    private String id;
    private String email;
    private String name;
    private String avatarUrl;
    private String role;
    private String authProvider;
    private boolean enabled;
    private CefrLevel englishLevel;
    private LocalDateTime createdAt;
    private long savedVocabCount;
    private String planCode;
    private String planName;
    private String subscriptionStatus;
}
