package com.viecultures.backend.modules.admin.service;

import com.viecultures.backend.modules.admin.dto.AdminUserSummaryDto;
import com.viecultures.backend.modules.admin.dto.LearnerDetailDto;
import com.viecultures.backend.entity.UserActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {
    Page<AdminUserSummaryDto> getUsers(String query, String role, Boolean enabled, Pageable pageable);
    Page<AdminUserSummaryDto> getUsers(String query, String role, Boolean enabled, String filterGroup, Pageable pageable);
    byte[] exportUsersCsv(String query, String role, Boolean enabled, String filterGroup);
    AdminUserSummaryDto getUserById(String id);
    AdminUserSummaryDto updateUserStatus(String id, boolean enabled);
    AdminUserSummaryDto updateUserStatus(String id, boolean enabled, String currentAdminEmail);
    AdminUserSummaryDto updateUserRole(String id, String role);
    AdminUserSummaryDto updateUserRole(String id, String role, String currentAdminEmail);

    LearnerDetailDto getLearnerDetails(String userId);
    Page<UserActivityLog> getLearnerActivityLogs(String userId, Pageable pageable);
}
