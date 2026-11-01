package com.viecultures.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_activity_logs", indexes = {
        @Index(name = "idx_user_activity_logs_created_at", columnList = "created_at"),
        @Index(name = "idx_user_activity_logs_event_type", columnList = "event_type"),
        @Index(name = "idx_user_activity_logs_event_category", columnList = "event_category"),
        @Index(name = "idx_user_activity_logs_user_id", columnList = "user_id"),
        @Index(name = "idx_user_activity_logs_anon_session", columnList = "anonymous_session_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "anonymous_session_id")
    private String anonymousSessionId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "event_category")
    private String eventCategory;

    @Column(name = "path", length = 1000)
    private String path;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
