package com.viecultures.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_streaks", indexes = {
        @Index(name = "idx_user_streak_user", columnList = "user_id", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStreak {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    @ToString.Exclude
    private User user;

    @Column(name = "streak_days", nullable = false)
    @Builder.Default
    private Integer streakDays = 0;

    @Column(name = "last_active_date")
    private LocalDate lastActiveDate;

    @Column(name = "streak_protected", nullable = false)
    @Builder.Default
    private Boolean streakProtected = false;

    @Column(name = "streak_freeze_count", nullable = false)
    @Builder.Default
    private Integer streakFreezeCount = 1;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
