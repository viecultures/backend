package com.viecultures.backend.controller;

import com.viecultures.backend.dto.CatalogResponse;
import com.viecultures.backend.annotation.AuditLog;
import com.viecultures.backend.dto.ReadingPostRequest;
import com.viecultures.backend.dto.ReadingPostResponse;
import com.viecultures.backend.entity.CefrLevel;
import com.viecultures.backend.service.ReadingPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "1. Reading Posts & Vocabularies", description = "Quản lý bài đọc song ngữ, các cặp đoạn văn và từ vựng học thuật")
public class ReadingPostController {

    private final ReadingPostService readingPostService;

    @Operation(summary = "Lấy danh mục bài đọc Discovery (Gom nhóm đa cấp độ)", description = "Gom nhóm theo topicGroup thành Lesson đa cấp độ (Level 1, 2, 3), kèm số lượng theo chủ đề (topicCounts) và phân trang")
    @GetMapping("/catalog")
    public ResponseEntity<CatalogResponse> getCatalog(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) Boolean standalone,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "12") Integer size) {
        CatalogResponse response = readingPostService.getCatalog(topic, level, query, region, sortBy, standalone, page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Lấy danh sách bài đọc", description = "Hỗ trợ lọc theo chủ đề (topic), vùng miền (region), nhóm bài (topicGroup), trình độ CEFR (level), bài đọc độc lập (standalone), hành trình (journeyId), tìm kiếm tự do theo từ khóa và tùy chọn nạp chi tiết từ vựng/câu hỏi (includeDetails)")
    @GetMapping
    public ResponseEntity<List<ReadingPostResponse>> getPosts(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String topicGroup,
            @RequestParam(required = false) CefrLevel level,
            @RequestParam(required = false) Boolean standalone,
            @RequestParam(required = false) String journeyId,
            @RequestParam(required = false) String provinceId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "false") Boolean includeDetails) {
        List<ReadingPostResponse> posts = readingPostService.getPosts(topic, level, query, region, topicGroup, standalone, journeyId, provinceId, includeDetails);
        return ResponseEntity.ok(posts);
    }

    @Operation(summary = "Xem chi tiết bài đọc", description = "Lấy đầy đủ thông tin bài đọc cùng danh sách đoạn văn song ngữ và từ vựng học thuật")
    @GetMapping("/{id}")
    public ResponseEntity<ReadingPostResponse> getPostById(@PathVariable String id) {
        ReadingPostResponse post = readingPostService.getPostById(id);
        return ResponseEntity.ok(post);
    }

    @Operation(summary = "Tạo mới bài đọc", description = "Tạo bài đọc mới kèm các đoạn song ngữ, từ vựng và gán vào hành trình nếu có")
    @AuditLog(action = "CREATE_POST", entity = "POST", description = "Tạo mới bài đọc song ngữ")
    @PostMapping
    public ResponseEntity<ReadingPostResponse> createPost(@RequestBody ReadingPostRequest request) {
        ReadingPostResponse created = readingPostService.createPost(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Cập nhật bài đọc", description = "Cập nhật thông tin bài đọc, danh sách đoạn văn, từ vựng và hành trình theo ID")
    @AuditLog(action = "UPDATE_POST", entity = "POST", description = "Cập nhật thông tin bài đọc song ngữ")
    @PutMapping("/{id}")
    public ResponseEntity<ReadingPostResponse> updatePost(
            @PathVariable String id,
            @RequestBody ReadingPostRequest request) {
        ReadingPostResponse updated = readingPostService.updatePost(id, request);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Xóa bài đọc", description = "Xóa hoàn toàn bài đọc và các đoạn song ngữ, từ vựng liên kết")
    @AuditLog(action = "DELETE_POST", entity = "POST", description = "Xóa hoàn toàn bài đọc song ngữ")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePost(@PathVariable String id) {
        readingPostService.deletePost(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa bài đọc ID: " + id));
    }

    @Operation(summary = "Tăng lượt đọc", description = "Cộng 1 vào tổng số lượt đọc bài viết")
    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> incrementReads(@PathVariable String id) {
        readingPostService.incrementReads(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Đã cập nhật lượt đọc bài viết"));
    }
}
