package com.viecultures.backend.repository;

import com.viecultures.backend.entity.DictionaryEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DictionaryEntryRepository extends JpaRepository<DictionaryEntry, String> {

    Optional<DictionaryEntry> findByWordIgnoreCase(String word);

    List<DictionaryEntry> findByWordStartingWithIgnoreCaseOrderBySearchCountDesc(String prefix, Pageable pageable);

    List<DictionaryEntry> findTop30ByOrderBySearchCountDesc();
}
