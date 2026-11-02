package com.viecultures.backend.repository;

import com.viecultures.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleId(String googleId);
    boolean existsByEmail(String email);

    @Query(value = "SELECT u FROM User u WHERE " +
           "(:query IS NULL OR :query = '' OR LOWER(COALESCE(u.name, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:role IS NULL OR :role = '' OR u.role = :role OR u.role = CONCAT('ROLE_', :role) OR REPLACE(u.role, 'ROLE_', '') = :role) AND " +
           "(:enabled IS NULL OR u.enabled = :enabled)",
           countQuery = "SELECT count(u) FROM User u WHERE " +
           "(:query IS NULL OR :query = '' OR LOWER(COALESCE(u.name, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:role IS NULL OR :role = '' OR u.role = :role OR u.role = CONCAT('ROLE_', :role) OR REPLACE(u.role, 'ROLE_', '') = :role) AND " +
           "(:enabled IS NULL OR u.enabled = :enabled)")
    Page<User> searchUsers(@Param("query") String query,
                           @Param("role") String role,
                           @Param("enabled") Boolean enabled,
                           Pageable pageable);

    @Query(value = "SELECT u FROM User u WHERE " +
           "(:query IS NULL OR :query = '' OR LOWER(COALESCE(u.name, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:role IS NULL OR :role = '' OR u.role = :role OR u.role = CONCAT('ROLE_', :role) OR REPLACE(u.role, 'ROLE_', '') = :role) AND " +
           "(:enabled IS NULL OR u.enabled = :enabled)")
    List<User> searchUsersList(@Param("query") String query,
                               @Param("role") String role,
                               @Param("enabled") Boolean enabled,
                               Sort sort);
}
