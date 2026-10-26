package com.viecultures.backend.repository;

import com.viecultures.backend.entity.UserStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserStreakRepository extends JpaRepository<UserStreak, String> {

    Optional<UserStreak> findByUserId(String userId);

    Optional<UserStreak> findByUserEmail(String email);
}
