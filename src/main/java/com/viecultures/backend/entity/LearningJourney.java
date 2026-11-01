package com.viecultures.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "learning_journeys")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningJourney {

    @Id
    @Column(nullable = false, unique = true)
    private String id; // Slug identifier (e.g. "pho-in-hanoi") or custom ID

    @Column(name = "title_en", nullable = false)
    private String titleEn;

    @Column(name = "title_vi", nullable = false)
    private String titleVi;

    @Column(nullable = false)
    private String theme; // cuisine, places, crafts, festivals, history

    @Column(name = "province_id", nullable = false)
    private String provinceId; // e.g. "ha-noi", "ninh-binh"

    @Column(nullable = false)
    private String region; // north, central, south

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "status")
    @Builder.Default
    private String status = "published"; // published, draft, upcoming

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "journey", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("chapterOrder ASC")
    @Builder.Default
    @ToString.Exclude
    private List<ReadingPost> chapters = new ArrayList<>();
}
