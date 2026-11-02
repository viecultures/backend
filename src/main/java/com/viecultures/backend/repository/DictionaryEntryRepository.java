package com.viecultures.backend.repository;

import com.viecultures.backend.entity.DictionaryEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DictionaryEntryRepository extends JpaRepository<DictionaryEntry, String>, JpaSpecificationExecutor<DictionaryEntry> {

    Optional<DictionaryEntry> findByWordIgnoreCase(String word);

    List<DictionaryEntry> findByWordStartingWithIgnoreCaseOrderBySearchCountDesc(String prefix, Pageable pageable);

    List<DictionaryEntry> findTop30ByOrderBySearchCountDesc();

    @Query(value = "SELECT d FROM DictionaryEntry d WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(d.word) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(COALESCE(d.viMeaning, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:level IS NULL OR :level = '' OR UPPER(COALESCE(d.level, '')) = UPPER(:level))",
           countQuery = "SELECT count(d) FROM DictionaryEntry d WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(d.word) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(COALESCE(d.viMeaning, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:level IS NULL OR :level = '' OR UPPER(COALESCE(d.level, '')) = UPPER(:level))")
    Page<DictionaryEntry> searchVocabularies(@Param("keyword") String keyword,
                                            @Param("level") String level,
                                            Pageable pageable);
}
