package com.viecultures.backend.modules.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectTransactionRequest {

    @NotBlank(message = "Lý do từ chối không được để trống")
    private String reason;
}
