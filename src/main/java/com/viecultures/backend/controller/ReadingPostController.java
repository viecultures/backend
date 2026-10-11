package com.viecultures.backend.controller;

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
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "1. Reading Posts & Vocabularies", description = "Quản lý bài đọc song ngữ, các cặp đoạn văn và từ vựng học thuật")
public class ReadingPostController {

    private final ReadingPostService readingPostService;

    @Operation(summary = "Lấy danh sách bài đọc", description = "Hỗ trợ lọc theo chủ đề (topic), vùng miền (region), nhóm bài (topicGroup), trình độ CEFR (level) và tìm kiếm tự do theo từ khóa")
    @GetMapping
    public ResponseEntity<List<ReadingPostResponse>> getPosts(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String topicGroup,
            @RequestParam(required = false) CefrLevel level,
            @RequestParam(required = false) String query) {
        List<ReadingPostResponse> posts = readingPostService.getPosts(topic, level, query, region, topicGroup);
        return ResponseEntity.ok(posts);
    }

    @Operation(summary = "Xem chi tiết bài đọc", description = "Lấy đầy đủ thông tin bài đọc cùng danh sách đoạn văn song ngữ và từ vựng học thuật")
    @GetMapping("/{id}")
    public ResponseEntity<ReadingPostResponse> getPostById(@PathVariable String id) {
        ReadingPostResponse post = readingPostService.getPostById(id);
        return ResponseEntity.ok(post);
    }

    @Operation(summary = "Tạo mới bài đọc", description = "Tạo bài đọc mới kèm các đoạn song ngữ và từ vựng trích xuất")
    @PostMapping
    public ResponseEntity<ReadingPostResponse> createPost(@RequestBody ReadingPostRequest request) {
        ReadingPostResponse created = readingPostService.createPost(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Cập nhật bài đọc", description = "Cập nhật thông tin bài đọc, danh sách đoạn văn và từ vựng theo ID")
    @PutMapping("/{id}")
    public ResponseEntity<ReadingPostResponse> updatePost(
            @PathVariable String id,
            @RequestBody ReadingPostRequest request) {
        ReadingPostResponse updated = readingPostService.updatePost(id, request);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Xóa bài đọc", description = "Xóa bài đọc và các dữ liệu liên kết liên quan theo ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePost(@PathVariable String id) {
        readingPostService.deletePost(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Xóa bài đọc thành công",
                "id", id
        ));
    }

    @Operation(summary = "Tăng lượt đọc bài", description = "Ghi nhận 1 lượt đọc mới khi người dùng mở bài")
    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> incrementReadCount(@PathVariable String id) {
        readingPostService.incrementReads(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Đã ghi nhận lượt đọc"));
    }
}
