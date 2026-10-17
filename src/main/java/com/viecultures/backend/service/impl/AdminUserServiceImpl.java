package com.viecultures.backend.service.impl;

import com.viecultures.backend.dto.admin.AdminUserSummaryDto;
import com.viecultures.backend.entity.User;
import com.viecultures.backend.repository.UserRepository;
import com.viecultures.backend.repository.UserVocabProgressRepository;
import com.viecultures.backend.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final UserVocabProgressRepository userVocabProgressRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserSummaryDto> getUsers(String query, String role, Boolean enabled, Pageable pageable) {
        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        String cleanRole = (role != null && !role.trim().isEmpty()) ? role.trim() : null;
        Page<User> users = userRepository.searchUsers(cleanQuery, cleanRole, enabled, pageable);
        return users.map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserSummaryDto getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại với ID: " + id));
        return toDto(user);
    }

    private static final java.util.Set<String> ALLOWED_ROLES = java.util.Set.of("ROLE_USER", "ROLE_ADMIN");

    @Override
    @Transactional
    public AdminUserSummaryDto updateUserStatus(String id, boolean enabled) {
        return updateUserStatus(id, enabled, null);
    }

    @Override
    @Transactional
    public AdminUserSummaryDto updateUserStatus(String id, boolean enabled, String currentAdminEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại với ID: " + id));

        String adminEmail = resolveCurrentAdminEmail(currentAdminEmail);

        // Ngăn chặn admin tự khóa tài khoản của chính mình
        if (!enabled && adminEmail != null && adminEmail.equalsIgnoreCase(user.getEmail())) {
            throw new IllegalStateException("Bạn không thể tự khóa tài khoản quản trị của chính mình để tránh mất quyền truy cập hệ thống.");
        }

        user.setEnabled(enabled);
        User saved = userRepository.save(user);
        log.info("Admin updated user status: id={}, email={}, enabled={}", id, user.getEmail(), enabled);
        return toDto(saved);
    }

    @Override
    @Transactional
    public AdminUserSummaryDto updateUserRole(String id, String role) {
        return updateUserRole(id, role, null);
    }

    @Override
    @Transactional
    public AdminUserSummaryDto updateUserRole(String id, String role, String currentAdminEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại với ID: " + id));

        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Vai trò không được để trống");
        }

        String normalizedRole = role.trim().toUpperCase();
        if (!normalizedRole.startsWith("ROLE_")) {
            normalizedRole = "ROLE_" + normalizedRole;
        }

        if (!ALLOWED_ROLES.contains(normalizedRole)) {
            throw new IllegalArgumentException("Vai trò không hợp lệ: '" + role + "'. Hệ thống chỉ chấp nhận: " + ALLOWED_ROLES);
        }

        String adminEmail = resolveCurrentAdminEmail(currentAdminEmail);

        // Ngăn chặn admin tự tước quyền ROLE_ADMIN của chính mình
        if (adminEmail != null && adminEmail.equalsIgnoreCase(user.getEmail())) {
            boolean isCurrentlyAdmin = "ROLE_ADMIN".equalsIgnoreCase(user.getRole()) || "ADMIN".equalsIgnoreCase(user.getRole());
            if (isCurrentlyAdmin && !"ROLE_ADMIN".equalsIgnoreCase(normalizedRole)) {
                throw new IllegalStateException("Bạn không thể tự tước quyền Quản trị viên (ROLE_ADMIN) của chính mình để tránh mất quyền quản trị.");
            }
        }

        user.setRole(normalizedRole);
        User saved = userRepository.save(user);
        log.info("Admin updated user role: id={}, email={}, role={}", id, user.getEmail(), normalizedRole);
        return toDto(saved);
    }

    private String resolveCurrentAdminEmail(String providedEmail) {
        if (providedEmail != null && !providedEmail.trim().isEmpty()) {
            return providedEmail.trim();
        }
        try {
            org.springframework.security.core.Authentication auth =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails ud) {
                    return ud.getUsername();
                } else if (auth.getPrincipal() instanceof String principalStr) {
                    return principalStr;
                }
            }
        } catch (Exception ex) {
            log.debug("Unable to resolve current admin email from SecurityContext: {}", ex.getMessage());
        }
        return null;
    }

    private AdminUserSummaryDto toDto(User user) {
        long vocabCount = 0;
        try {
            vocabCount = userVocabProgressRepository.countByUserId(user.getId());
        } catch (Exception e) {
            log.warn("Could not get vocab count for user {}: {}", user.getId(), e.getMessage());
        }

        return AdminUserSummaryDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .authProvider(user.getAuthProvider())
                .enabled(user.isEnabled())
                .englishLevel(user.getEnglishLevel())
                .createdAt(user.getCreatedAt())
                .savedVocabCount(vocabCount)
                .build();
    }
}
