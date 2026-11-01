package com.viecultures.backend.repository;

import com.viecultures.backend.entity.UserActivityLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, String>, JpaSpecificationExecutor<UserActivityLog> {

    @Query("SELECT l.eventCategory, COUNT(l) FROM UserActivityLog l WHERE l.eventCategory IS NOT NULL GROUP BY l.eventCategory ORDER BY COUNT(l) DESC")
    List<Object[]> countGroupedByEventCategory();

    @Query("SELECT l.eventType, COUNT(l) FROM UserActivityLog l WHERE l.eventType IS NOT NULL GROUP BY l.eventType ORDER BY COUNT(l) DESC")
    List<Object[]> countGroupedByEventType();

    @Query("SELECT l.path, COUNT(l) FROM UserActivityLog l WHERE (l.eventType = 'ARTICLE_READ' OR l.eventCategory = 'READER') AND l.path IS NOT NULL AND TRIM(l.path) <> '' GROUP BY l.path ORDER BY COUNT(l) DESC")
    List<Object[]> findTopReadPaths(Pageable pageable);

    @Query("SELECT l.payload FROM UserActivityLog l WHERE (l.eventType = 'VOCAB_LOOKUP' OR l.eventCategory = 'VOCABULARY') AND l.payload IS NOT NULL AND TRIM(l.payload) <> ''")
    List<String> findRecentVocabPayloads(Pageable pageable);

    @Query("SELECT l.payload FROM UserActivityLog l WHERE (l.eventType = 'ARTICLE_READ' OR l.eventCategory = 'READER') AND l.payload IS NOT NULL AND TRIM(l.payload) <> ''")
    List<String> findRecentArticlePayloads(Pageable pageable);
}
