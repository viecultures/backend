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

    @Column(name = "topic_group")
    private String topicGroup;

    @Column(name = "title_en", nullable = false)
    private String titleEn;

    @Column(name = "title_vi", nullable = false)
    private String titleVi;

    @Column(nullable = false)
    private String topic;

    @Column(name = "region")
    private String region;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "content_en", columnDefinition = "TEXT")
    private String contentEn;

    @Column(name = "content_vi", columnDefinition = "TEXT")
    private String contentVi;

    @Column(name = "shadowing_audio_url")
    private String shadowingAudioUrl;

    @Column(name = "audio_tracks", columnDefinition = "TEXT")
    private String audioTracks;

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

    @Column(name = "featured")
    @Builder.Default
    private Boolean featured = false;

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
