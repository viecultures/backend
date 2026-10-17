package com.viecultures.backend.service;

import com.viecultures.backend.dto.admin.AdminUserSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {
    Page<AdminUserSummaryDto> getUsers(String query, String role, Boolean enabled, Pageable pageable);
    AdminUserSummaryDto getUserById(String id);
    AdminUserSummaryDto updateUserStatus(String id, boolean enabled);
    AdminUserSummaryDto updateUserStatus(String id, boolean enabled, String currentAdminEmail);
    AdminUserSummaryDto updateUserRole(String id, String role);
    AdminUserSummaryDto updateUserRole(String id, String role, String currentAdminEmail);
}
