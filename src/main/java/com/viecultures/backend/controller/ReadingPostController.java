package com.viecultures.backend.controller;

import com.viecultures.backend.dto.ReadingPostRequest;
import com.viecultures.backend.dto.ReadingPostResponse;
import com.viecultures.backend.entity.CefrLevel;
import com.viecultures.backend.service.ReadingPostService;
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
public class ReadingPostController {

    private final ReadingPostService readingPostService;

    /**
     * Lấy danh sách bài đọc (hỗ trợ lọc theo topic, level, và từ khóa tìm kiếm)
     */
    @GetMapping
    public ResponseEntity<List<ReadingPostResponse>> getPosts(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) CefrLevel level,
            @RequestParam(required = false) String query) {
        List<ReadingPostResponse> posts = readingPostService.getPosts(topic, level, query);
        return ResponseEntity.ok(posts);
    }

    /**
     * Xem chi tiết 1 bài đọc (kèm danh sách đoạn song ngữ và từ vựng ngữ cảnh)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReadingPostResponse> getPostById(@PathVariable String id) {
        ReadingPostResponse post = readingPostService.getPostById(id);
        return ResponseEntity.ok(post);
    }

    /**
     * Tạo bài đọc mới
     */
    @PostMapping
    public ResponseEntity<ReadingPostResponse> createPost(@RequestBody ReadingPostRequest request) {
        ReadingPostResponse created = readingPostService.createPost(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Cập nhật bài đọc theo ID
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReadingPostResponse> updatePost(
            @PathVariable String id,
            @RequestBody ReadingPostRequest request) {
        ReadingPostResponse updated = readingPostService.updatePost(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Xóa bài đọc theo ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePost(@PathVariable String id) {
        readingPostService.deletePost(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Xóa bài đọc thành công",
                "id", id
        ));
    }

    /**
     * Tăng số lượt đọc khi người dùng mở bài
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> incrementReadCount(@PathVariable String id) {
        readingPostService.incrementReads(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Đã ghi nhận lượt đọc"));
    }
}
