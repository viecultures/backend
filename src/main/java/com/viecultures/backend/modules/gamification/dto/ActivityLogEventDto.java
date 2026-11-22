package com.viecultures.backend.modules.gamification.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogEventDto {

    @NotBlank(message = "Event type must not be blank")
    private String eventType;

    private String eventCategory;

    private String path;

    private JsonNode payload;

    private Long clientTimestamp;
}
