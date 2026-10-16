package com.viecultures.backend.dto;

import com.viecultures.backend.entity.CefrLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả xác thực thành công chứa JWT và thông tin tài khoản")
public class AuthResponse {

    @Schema(description = "Chuỗi JWT Bearer token")
    private String accessToken;

    @Builder.Default
    @Schema(example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "Thời gian hết hạn của token (tính bằng mili-giây)", example = "604800000")
    private long expiresIn;

    @Schema(description = "Thông tin tóm tắt của người dùng đăng nhập")
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String id;
        private String email;
        private String name;
        private String avatarUrl;
        private String role;
        private String authProvider;
        private CefrLevel englishLevel;
        private LocalDateTime createdAt;
    }
}
