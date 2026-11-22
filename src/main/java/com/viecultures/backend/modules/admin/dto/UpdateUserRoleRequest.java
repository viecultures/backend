package com.viecultures.backend.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRoleRequest {
    @NotBlank(message = "Vai trò không được để trống")
    @Pattern(regexp = "^(?i)(ROLE_)?(USER|ADMIN)$", message = "Vai trò chỉ có thể là ROLE_USER hoặc ROLE_ADMIN")
    private String role;
}
