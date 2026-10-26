package com.viecultures.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_seals", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_post_seal_date", columnNames = {"user_id", "reading_post_id", "sealed_date"})
}, indexes = {
        @Index(name = "idx_user_seals_user_id", columnList = "user_id"),
        @Index(name = "idx_user_seals_post_id", columnList = "reading_post_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSeal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private User user;

    @Column(name = "reading_post_id", nullable = false)
    private String readingPostId;

    @CreationTimestamp
    @Column(name = "sealed_at", nullable = false, updatable = false)
    private LocalDateTime sealedAt;

    @Column(name = "sealed_date", nullable = false)
    private LocalDate sealedDate;

    @Column(name = "words_learned_count")
    @Builder.Default
    private Integer wordsLearnedCount = 0;

    @Column(name = "source", length = 50)
    @Builder.Default
    private String source = "reader";
}
