package com.viecultures.backend.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu đăng nhập bằng Email và Mật khẩu")
public class AuthRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng email không hợp lệ")
    @Schema(example = "user@viecultures.com")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Schema(example = "Secret123@")
    private String password;
}
