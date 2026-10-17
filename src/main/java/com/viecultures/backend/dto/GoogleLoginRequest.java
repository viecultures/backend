package com.viecultures.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu đăng nhập hoặc liên kết bằng Google OAuth (Web/Mobile)")
public class GoogleLoginRequest {

    @NotBlank(message = "Google ID token không được để trống")
    @Schema(description = "Google ID Token (JWT do Google Sign-In cấp)")
    private String idToken;

    @Schema(description = "Google Access Token (nếu client sử dụng access token thay vì id token)")
    private String accessToken;

    @Schema(description = "Email người dùng Google (tùy chọn hoặc dùng cho mock/fallback)", example = "user.google@gmail.com")
    private String email;

    @Schema(description = "Tên hiển thị từ hồ sơ Google", example = "Ninh Thiên Luân")
    private String name;

    @Schema(description = "Ảnh đại diện avatar từ Google", example = "https://lh3.googleusercontent.com/...")
    private String avatarUrl;

    @Schema(description = "Google Subject / User ID", example = "109876543210987654321")
    private String googleId;
}
