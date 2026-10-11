package com.viecultures.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "paragraph_pairs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParagraphPair {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private ReadingPost readingPost;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "en_text", nullable = false, columnDefinition = "TEXT")
    private String enText;

    @Column(name = "vi_text", nullable = false, columnDefinition = "TEXT")
    private String viText;

    @Column(name = "audio_timestamp_start")
    private Float audioTimestampStart;

    @Column(name = "audio_timestamp_end")
    private Float audioTimestampEnd;
}
