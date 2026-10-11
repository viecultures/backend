package com.viecultures.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reading_posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingPost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "title_en", nullable = false)
    private String titleEn;

    @Column(name = "title_vi", nullable = false)
    private String titleVi;

    @Column(nullable = false)
    private String topic;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "shadowing_audio_url")
    private String shadowingAudioUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CefrLevel level;

    @Column(name = "read_time")
    private String readTime;

    @Column(name = "total_reads")
    @Builder.Default
    private Integer totalReads = 0;

    @Column(name = "likes_count")
    @Builder.Default
    private Integer likesCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "readingPost", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    @ToString.Exclude
    private List<ParagraphPair> paragraphs = new ArrayList<>();

    @OneToMany(mappedBy = "readingPost", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<VocabItem> vocabularies = new ArrayList<>();

    @OneToMany(mappedBy = "readingPost", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<UserReflection> reflections = new ArrayList<>();
}
