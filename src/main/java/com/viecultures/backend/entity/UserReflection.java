package com.viecultures.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_reflections")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReflection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private ReadingPost readingPost;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ElementCollection
    @CollectionTable(name = "reflection_vocab_ids", joinColumns = @JoinColumn(name = "reflection_id"))
    @Column(name = "vocab_id")
    @Builder.Default
    private List<String> usedVocabIds = new ArrayList<>();

    @Column(name = "reactions_count")
    @Builder.Default
    private Integer reactionsCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReflectionStatus status = ReflectionStatus.PUBLISHED;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
