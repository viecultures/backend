package com.viecultures.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_reflection_reactions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_reflection_reaction", columnNames = {"user_id", "reflection_id"})
    },
    indexes = {
        @Index(name = "idx_reaction_user", columnList = "user_id"),
        @Index(name = "idx_reaction_reflection", columnList = "reflection_id")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReflectionReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reflection_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private UserReflection reflection;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
