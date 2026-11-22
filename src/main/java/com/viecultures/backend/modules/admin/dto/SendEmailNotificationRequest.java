package com.viecultures.backend.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu gửi email thông báo hệ thống")
public class SendEmailNotificationRequest {

    @Schema(description = "Địa chỉ email người nhận", example = "student@viecultures.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String toEmail;

    @Schema(description = "Tiêu đề email (Subject)", example = "VieCultures - Bài đọc văn hóa mới tuần này")
    private String subject;

    @Schema(description = "Tiêu đề lớn trong thân email (Headline)", example = "Khám phá Nhã Nhạc Cung Đình Huế")
    private String headline;

    @Schema(description = "Nội dung thông báo (hỗ trợ định dạng HTML)", example = "<p>Bài đọc mới đã được xuất bản với trọn bộ bài tập từ vựng học thuật B2-C1.</p>")
    private String messageContent;

    @Schema(description = "Đường link hành động khi bấm nút (tùy chọn)", example = "https://viecultures.com/discovery")
    private String actionUrl;

    @Schema(description = "Nhãn hiển thị trên nút hành động (tùy chọn)", example = "Đọc Bài Ngay")
    private String actionText;

    @Schema(description = "Tên người nhận (tùy chọn)", example = "Nguyễn Văn A")
    private String recipientName;
}
