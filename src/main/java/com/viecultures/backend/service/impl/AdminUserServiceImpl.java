package com.viecultures.backend.service.impl;

import com.viecultures.backend.dto.admin.AdminUserSummaryDto;
import com.viecultures.backend.entity.User;
import com.viecultures.backend.repository.UserRepository;
import com.viecultures.backend.repository.UserVocabProgressRepository;
import com.viecultures.backend.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final UserVocabProgressRepository userVocabProgressRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserSummaryDto> getUsers(String query, String role, Boolean enabled, Pageable pageable) {
        return getUsers(query, role, enabled, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserSummaryDto> getUsers(String query, String role, Boolean enabled, String filterGroup, Pageable pageable) {
        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        String cleanRole = (role != null && !role.trim().isEmpty()) ? role.trim() : null;
        String cleanFilterGroup = (filterGroup != null && !filterGroup.trim().isEmpty()) ? filterGroup.trim() : null;

        if (cleanFilterGroup == null || "all".equalsIgnoreCase(cleanFilterGroup)) {
            Page<User> users = userRepository.searchUsers(cleanQuery, cleanRole, enabled, pageable);
            return users.map(this::toDto);
        }

        Sort sort = (pageable != null && pageable.getSort().isSorted())
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "createdAt");
        List<User> userList = userRepository.searchUsersList(cleanQuery, cleanRole, enabled, sort);

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<AdminUserSummaryDto> filtered = userList.stream()
                .map(this::toDto)
                .filter(dto -> matchesFilterGroup(dto, cleanFilterGroup, sevenDaysAgo))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<AdminUserSummaryDto> subList = (start <= end && start < filtered.size())
                ? filtered.subList(start, end)
                : Collections.emptyList();

        return new PageImpl<>(subList, pageable, filtered.size());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportUsersCsv(String query, String role, Boolean enabled, String filterGroup) {
        log.info("Generating CSV export for users: query={}, role={}, enabled={}, filterGroup={}",
                query, role, enabled, filterGroup);
        try {
            String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
            String cleanRole = (role != null && !role.trim().isEmpty()) ? role.trim() : null;
            String cleanFilterGroup = (filterGroup != null && !filterGroup.trim().isEmpty()) ? filterGroup.trim() : null;

            List<User> userList = userRepository.searchUsersList(
                    cleanQuery, cleanRole, enabled, Sort.by(Sort.Direction.DESC, "createdAt"));

            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            List<AdminUserSummaryDto> dtos = userList.stream()
                    .map(this::toDto)
                    .filter(dto -> matchesFilterGroup(dto, cleanFilterGroup, sevenDaysAgo))
                    .toList();

            StringBuilder sb = new StringBuilder();
            // BOM for UTF-8 in Excel
            sb.append('\uFEFF');

            // Header: 'ID', 'Họ và tên', 'Email', 'Vai trò', 'Trình độ CEFR', 'Từ đã lưu', 'Phương thức đăng nhập', 'Ngày tạo', 'Trạng thái'
            sb.append("ID,Họ và tên,Email,Vai trò,Trình độ CEFR,Từ đã lưu,Phương thức đăng nhập,Ngày tạo,Trạng thái\r\n");

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

            for (AdminUserSummaryDto u : dtos) {
                String roleDisplay = "ROLE_ADMIN".equalsIgnoreCase(u.getRole()) || "ADMIN".equalsIgnoreCase(u.getRole())
                        ? "Quản trị viên"
                        : "Học viên";
                String cefrDisplay = u.getEnglishLevel() != null ? u.getEnglishLevel().name() : "Chưa xác định";
                String statusDisplay = u.isEnabled() ? "Đang hoạt động" : "Đã khóa";
                String createdAtDisplay = u.getCreatedAt() != null ? u.getCreatedAt().format(dtf) : "";

                sb.append(escapeCsv(u.getId())).append(',')
                  .append(escapeCsv(u.getName())).append(',')
                  .append(escapeCsv(u.getEmail())).append(',')
                  .append(escapeCsv(roleDisplay)).append(',')
                  .append(escapeCsv(cefrDisplay)).append(',')
                  .append(u.getSavedVocabCount()).append(',')
                  .append(escapeCsv(u.getAuthProvider())).append(',')
                  .append(escapeCsv(createdAtDisplay)).append(',')
                  .append(escapeCsv(statusDisplay)).append("\r\n");
            }

            return sb.toString().getBytes(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.error("Error occurred while generating users CSV export: {}", ex.getMessage(), ex);
            return ("\uFEFFID,Họ và tên,Email,Vai trò,Trình độ CEFR,Từ đã lưu,Phương thức đăng nhập,Ngày tạo,Trạng thái\r\n")
                    .getBytes(StandardCharsets.UTF_8);
        }
    }

    private boolean matchesFilterGroup(AdminUserSummaryDto dto, String filterGroup, LocalDateTime sevenDaysAgo) {
        if (filterGroup == null || filterGroup.isBlank() || "all".equalsIgnoreCase(filterGroup)) {
            return true;
        }
        String fg = filterGroup.trim().toLowerCase();
        switch (fg) {
            case "recent_7d":
                return dto.getCreatedAt() != null && dto.getCreatedAt().isAfter(sevenDaysAgo);
            case "admin":
                return dto.getRole() != null && dto.getRole().toUpperCase().contains("ADMIN");
            case "active":
                return dto.getSavedVocabCount() > 0 || dto.isEnabled();
            case "inactive":
                return dto.getSavedVocabCount() == 0 || !dto.isEnabled();
            default:
                return true;
        }
    }

    private String escapeCsv(Object val) {
        if (val == null) return "";
        String str = String.valueOf(val);
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
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

        // Ngan chan admin tu khoa tai khoan cua chinh minh
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

        // Ngan chan admin tu tuoc quyen ROLE_ADMIN cua chinh minh
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
