package com.viecultures.backend.repository;

import com.viecultures.backend.entity.EmailOtpToken;
import com.viecultures.backend.entity.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailOtpTokenRepository extends JpaRepository<EmailOtpToken, Long> {

    Optional<EmailOtpToken> findTopByEmailAndPurposeOrderByIdDesc(String email, OtpPurpose purpose);

    void deleteAllByEmailAndPurpose(String email, OtpPurpose purpose);
}
