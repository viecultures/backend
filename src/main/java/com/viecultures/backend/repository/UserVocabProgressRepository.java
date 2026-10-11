package com.viecultures.backend.repository;

import com.viecultures.backend.entity.UserVocabProgress;
import com.viecultures.backend.entity.VocabProgressStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserVocabProgressRepository extends JpaRepository<UserVocabProgress, UserVocabProgress.UserVocabProgressId> {
    List<UserVocabProgress> findByUserIdAndStatus(String userId, VocabProgressStatus status);
    List<UserVocabProgress> findByUserId(String userId);
}
