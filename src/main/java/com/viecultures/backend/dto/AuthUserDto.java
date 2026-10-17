package com.viecultures.backend.dto;

import com.viecultures.backend.entity.CefrLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin người dùng sau khi xác thực")
public class AuthUserDto {

    @Schema(description = "User ID (UUID)")
    private String id;

    @Schema(description = "Địa chỉ email")
    private String email;

    @Schema(description = "Họ và tên")
    private String name;

    @Schema(description = "URL ảnh đại diện")
    private String avatarUrl;

    @Schema(description = "Vai trò trong hệ thống", example = "STUDENT")
    private String role;

    @Schema(description = "Nhà cung cấp xác thực (LOCAL, GOOGLE)", example = "GOOGLE")
    private String authProvider;

    @Schema(description = "Trình độ tiếng Anh CEFR", example = "A1")
    private CefrLevel englishLevel;
}
