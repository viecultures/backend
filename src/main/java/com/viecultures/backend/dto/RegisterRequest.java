package com.viecultures.backend.dto;

import com.viecultures.backend.entity.CefrLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu đăng ký tài khoản mới")
public class RegisterRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng email không hợp lệ")
    @Schema(example = "learner@viecultures.com")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu tối thiểu phải từ 6 ký tự")
    @Schema(example = "Secret123@")
    private String password;

    @NotBlank(message = "Họ và tên không được để trống")
    @Schema(example = "Nguyễn Văn An")
    private String name;

    @Schema(example = "B1", description = "Trình độ tiếng Anh theo chuẩn CEFR (A1, A2, B1, B2, C1, C2)")
    private CefrLevel englishLevel;
}
