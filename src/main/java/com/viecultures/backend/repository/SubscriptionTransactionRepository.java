package com.viecultures.backend.repository;

import com.viecultures.backend.entity.SubscriptionTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionTransactionRepository extends JpaRepository<SubscriptionTransaction, String> {

    Optional<SubscriptionTransaction> findByTransactionCode(String transactionCode);

    List<SubscriptionTransaction> findAllByOrderByCreatedAtDesc();

    List<SubscriptionTransaction> findByStatusOrderByCreatedAtDesc(String status);

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(t.amountVnd), 0) FROM SubscriptionTransaction t WHERE t.status = 'APPROVED'")
    Long sumTotalApprovedRevenue();

    @Query("SELECT t FROM SubscriptionTransaction t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:query IS NULL OR LOWER(t.userEmail) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(t.userName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(t.transactionCode) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(t.transferContent) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY t.createdAt DESC")
    List<SubscriptionTransaction> searchTransactions(@Param("status") String status, @Param("query") String query);
}
