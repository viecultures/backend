package com.viecultures.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Lesson referenced by AI Companion during cultural explanation")
public class ReferencedLessonDto {

    @Schema(description = "Unique ID of the reading post", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "Title of the reading post in English", example = "Pho Hanoi - The Essence of Northern Broth")
    private String titleEn;

    @Schema(description = "Title of the reading post in Vietnamese", example = "Phở Hà Nội - Tinh hoa ẩm thực Bắc Bộ")
    private String titleVi;

    @Schema(description = "Cultural topic category", example = "TOPIC_CUISINE")
    private String topic;

    @Schema(description = "Geographic region", example = "REGION_NORTH")
    private String region;
}
