package com.viecultures.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionTransactionDto {
    private String id;
    private String transactionCode;
    private String userId;
    private String userEmail;
    private String userName;
    private String planCode;
    private String planName;
    private Long amountVnd;
    private String transferContent;
    private String status;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
}
