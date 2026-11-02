package com.viecultures.backend.repository;

import com.viecultures.backend.entity.CefrLevel;
import com.viecultures.backend.entity.ReadingPost;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReadingPostRepository extends JpaRepository<ReadingPost, String>, JpaSpecificationExecutor<ReadingPost> {
    List<ReadingPost> findByTopicIgnoreCase(String topic);
    List<ReadingPost> findByLevel(CefrLevel level);

    @Query("SELECT p FROM ReadingPost p WHERE " +
           "LOWER(p.titleEn) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.titleVi) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.topic) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.summary) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<ReadingPost> searchPosts(@Param("query") String query, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.totalReads), 0) FROM ReadingPost p")
    Long sumTotalReads();

    @Query("SELECT p.level, COUNT(p) FROM ReadingPost p WHERE p.level IS NOT NULL GROUP BY p.level")
    List<Object[]> countGroupByLevel();

    @Query("SELECT COALESCE(NULLIF(p.region, ''), 'Toàn quốc'), COUNT(p) FROM ReadingPost p GROUP BY COALESCE(NULLIF(p.region, ''), 'Toàn quốc')")
    List<Object[]> countGroupByRegion();
}
