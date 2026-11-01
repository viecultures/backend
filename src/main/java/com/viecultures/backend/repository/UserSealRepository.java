package com.viecultures.backend.repository;

import com.viecultures.backend.entity.UserSeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSealRepository extends JpaRepository<UserSeal, String> {

    Optional<UserSeal> findByUserIdAndReadingPostIdAndSealedDate(String userId, String readingPostId, LocalDate sealedDate);

    boolean existsByUserIdAndReadingPostIdAndSealedDate(String userId, String readingPostId, LocalDate sealedDate);

    List<UserSeal> findByUserIdOrderBySealedAtDesc(String userId);

    long countByUserId(String userId);
}
