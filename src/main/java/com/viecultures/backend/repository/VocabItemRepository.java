package com.viecultures.backend.repository;

import com.viecultures.backend.entity.VocabItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface VocabItemRepository extends JpaRepository<VocabItem, String>, JpaSpecificationExecutor<VocabItem> {

    List<VocabItem> findByReadingPostId(String postId);

    Page<VocabItem> findByReadingPostId(String postId, Pageable pageable);

    Optional<VocabItem> findFirstByWordIgnoreCase(String word);

    List<VocabItem> findByWordIgnoreCase(String word);

    Optional<VocabItem> findByReadingPostIdAndWordIgnoreCase(String postId, String word);

    @Query("SELECT v.readingPost.id, COUNT(v.id) FROM VocabItem v GROUP BY v.readingPost.id")
    List<Object[]> countVocabulariesGroupedByPostId();

    @Query("SELECT v.readingPost.id, COUNT(v.id) FROM VocabItem v WHERE v.readingPost.id IN :postIds GROUP BY v.readingPost.id")
    List<Object[]> countVocabulariesByPostIds(@Param("postIds") Collection<String> postIds);
}
