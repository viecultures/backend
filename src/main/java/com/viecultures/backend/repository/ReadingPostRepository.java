package com.viecultures.backend.repository;

import com.viecultures.backend.modules.learning.dto.CatalogPostView;
import com.viecultures.backend.modules.learning.dto.EntityOptionDto;
import com.viecultures.backend.modules.learning.dto.JourneyChapterView;
import com.viecultures.backend.entity.CefrLevel;
import com.viecultures.backend.entity.ReadingPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ReadingPostRepository extends JpaRepository<ReadingPost, String>, JpaSpecificationExecutor<ReadingPost> {
    List<ReadingPost> findByTopicIgnoreCase(String topic);
    List<ReadingPost> findByLevel(CefrLevel level);

    @Query("SELECT new com.viecultures.backend.modules.learning.dto.CatalogPostView(" +
           "p.id, p.topicGroup, p.titleEn, p.titleVi, p.topic, p.region, p.imageUrl, " +
           "p.wordCount, p.summary, p.summaryVi, p.level, p.readTime, p.totalReads, p.likesCount, " +
           "p.featured, p.createdAt, j.id) " +
           "FROM ReadingPost p LEFT JOIN p.journey j " +
           "WHERE (p.status IS NULL OR LOWER(p.status) = 'published') " +
           "AND (:standalone IS NULL OR (:standalone = true AND j.id IS NULL) OR (:standalone = false AND j.id IS NOT NULL))")
    List<CatalogPostView> findCatalogPostViews(@Param("standalone") Boolean standalone);

    @Query(value = "SELECT new com.viecultures.backend.modules.learning.dto.CatalogPostView(" +
           "p.id, p.topicGroup, p.titleEn, p.titleVi, p.topic, p.region, p.imageUrl, " +
           "p.wordCount, p.summary, p.summaryVi, p.level, p.readTime, p.totalReads, p.likesCount, " +
           "p.featured, p.createdAt, j.id) " +
           "FROM ReadingPost p LEFT JOIN p.journey j",
           countQuery = "SELECT COUNT(p) FROM ReadingPost p")
    Page<CatalogPostView> findAllCatalogPostViews(Pageable pageable);

    @Query("SELECT p FROM ReadingPost p LEFT JOIN FETCH p.journey")
    List<ReadingPost> findAllWithJourney();

    @Query("SELECT p FROM ReadingPost p LEFT JOIN FETCH p.journey WHERE p.journey IS NULL")
    List<ReadingPost> findStandaloneWithJourney();

    @Query("SELECT p FROM ReadingPost p WHERE " +
           "LOWER(p.titleEn) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.titleVi) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.topic) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.summary) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.summaryVi) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<ReadingPost> searchPosts(@Param("query") String query, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.totalReads), 0) FROM ReadingPost p")
    Long sumTotalReads();

    @Query("SELECT p.level, COUNT(p) FROM ReadingPost p WHERE p.level IS NOT NULL GROUP BY p.level")
    List<Object[]> countGroupByLevel();

    @Query("SELECT COALESCE(NULLIF(p.region, ''), 'Toàn quốc'), COUNT(p) FROM ReadingPost p GROUP BY COALESCE(NULLIF(p.region, ''), 'Toàn quốc')")
    List<Object[]> countGroupByRegion();

    @Query("SELECT new com.viecultures.backend.modules.learning.dto.JourneyChapterView(" +
           "p.id, p.journey.id, p.chapterOrder, p.topicGroup, " +
           "p.titleEn, p.titleVi, p.summary, p.readTime, p.level, p.imageUrl) " +
           "FROM ReadingPost p " +
           "WHERE p.journey.id IN :journeyIds " +
           "AND (p.status IS NULL OR LOWER(p.status) = 'published') " +
           "ORDER BY p.chapterOrder ASC")
    List<JourneyChapterView> findPublishedChapterViewsByJourneyIds(@Param("journeyIds") Collection<String> journeyIds);

    @Query("SELECT new com.viecultures.backend.modules.learning.dto.EntityOptionDto(p.id, p.titleVi, p.titleEn) " +
           "FROM ReadingPost p " +
           "WHERE (p.status IS NULL OR LOWER(p.status) = 'published') " +
           "ORDER BY p.titleVi ASC")
    List<EntityOptionDto> findAllPostOptions();
}
