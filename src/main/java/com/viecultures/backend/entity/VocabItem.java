package com.viecultures.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vocab_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private ReadingPost readingPost;

    @Column(nullable = false)
    private String word;

    private String ipa;

    @Column(nullable = false)
    private String pos; // Part of speech: noun, verb, adj, adv...

    @Column(name = "vi_meaning", nullable = false)
    private String viMeaning;

    @Column(name = "context_sentence", nullable = false, columnDefinition = "TEXT")
    private String contextSentence;

    @Column(name = "audio_url")
    private String audioUrl;

    @Enumerated(EnumType.STRING)
    private CefrLevel level;

    @Column(name = "usage_note", columnDefinition = "TEXT")
    private String usageNote;

    @OneToMany(mappedBy = "vocabItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<UserVocabProgress> userProgresses = new ArrayList<>();
}
