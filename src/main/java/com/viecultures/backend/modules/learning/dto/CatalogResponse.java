package com.viecultures.backend.modules.learning.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogResponse {
    private List<CatalogLessonDto> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private Map<String, Long> topicCounts;
}
