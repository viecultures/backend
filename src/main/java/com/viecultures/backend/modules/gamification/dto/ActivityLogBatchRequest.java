package com.viecultures.backend.modules.gamification.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogBatchRequest {

    private String anonymousSessionId;

    private String userId;

    private String userEmail;

    @Valid
    @NotEmpty(message = "Events list cannot be empty")
    @Builder.Default
    private List<ActivityLogEventDto> events = new ArrayList<>();
}
