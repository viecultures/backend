package com.viecultures.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_vocab_progress")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVocabProgress {

    @EmbeddedId
    private UserVocabProgressId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("vocabId")
    @JoinColumn(name = "vocab_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private VocabItem vocabItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VocabProgressStatus status = VocabProgressStatus.LEARNING;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserVocabProgressId implements Serializable {
        @Column(name = "user_id")
        private String userId;

        @Column(name = "vocab_id")
        private String vocabId;
    }
}
