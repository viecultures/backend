package com.viecultures.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_transactions", indexes = {
    @Index(name = "idx_sub_tx_code", columnList = "transaction_code", unique = true),
    @Index(name = "idx_sub_tx_status", columnList = "status"),
    @Index(name = "idx_sub_tx_user_email", columnList = "user_email")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "transaction_code", nullable = false, unique = true, length = 100)
    private String transactionCode;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "plan_code", nullable = false, length = 50)
    private String planCode;

    @Column(name = "plan_name", nullable = false)
    private String planName;

    @Column(name = "amount_vnd", nullable = false)
    private Long amountVnd;

    @Column(name = "transfer_content", nullable = false, columnDefinition = "TEXT")
    private String transferContent;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}
