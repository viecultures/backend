package com.viecultures.backend.controller;

import com.viecultures.backend.dto.EntityOptionDto;
import com.viecultures.backend.dto.LearningJourneyRequest;
import com.viecultures.backend.dto.LearningJourneyResponse;
import com.viecultures.backend.dto.PagedLearningJourneyResponse;
import com.viecultures.backend.service.LearningJourneyService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/journeys")
@RequiredArgsConstructor
@Tag(name = "1.1 Learning Journeys", description = "Quản lý chuỗi bài đọc theo hành trình học tập văn hóa và địa phương")
public class LearningJourneyController {

    private final LearningJourneyService journeyService;

    @Operation(summary = "Lấy danh sách rút gọn hành trình học cho dropdown", description = "Chỉ trả về id, titleVi, titleEn để tối ưu băng thông")
    @GetMapping("/options")
    public ResponseEntity<List<EntityOptionDto>> getJourneyOptions() {
        return ResponseEntity.ok(journeyService.getJourneyOptions());
    }

    @Operation(summary = "Lấy danh sách hành trình học", description = "Lọc theo vùng miền (region), tỉnh thành (provinceId), chủ đề (theme), trạng thái (status), từ khóa (query), hỗ trợ phân trang khi truyền page")
    @GetMapping
    public ResponseEntity<?> getJourneys(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String provinceId,
            @RequestParam(required = false) String theme,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null) {
            int pageSize = (size != null && size > 0) ? size : 6;
            PagedLearningJourneyResponse paged = journeyService.getPagedJourneys(region, provinceId, theme, status, query, page, pageSize);
            return ResponseEntity.ok(paged);
        }
        List<LearningJourneyResponse> journeys = journeyService.getJourneys(region, provinceId, theme, status, query);
        return ResponseEntity.ok(journeys);
    }

    @Operation(summary = "Chi tiết hành trình học", description = "Lấy chi tiết hành trình cùng danh sách các chặng bài đọc theo thứ tự")
    @GetMapping("/{id}")
    public ResponseEntity<LearningJourneyResponse> getJourneyById(@PathVariable String id) {
        LearningJourneyResponse journey = journeyService.getJourneyById(id);
        return ResponseEntity.ok(journey);
    }

    @Operation(summary = "Tạo mới hành trình học", description = "Tạo hành trình mới kèm gán danh sách bài đọc theo thứ tự chặng")
    @PostMapping
    public ResponseEntity<LearningJourneyResponse> createJourney(@RequestBody LearningJourneyRequest request) {
        LearningJourneyResponse created = journeyService.createJourney(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Cập nhật hành trình học", description = "Cập nhật metadata và thứ tự các chặng bài đọc")
    @PutMapping("/{id}")
    public ResponseEntity<LearningJourneyResponse> updateJourney(
            @PathVariable String id,
            @RequestBody LearningJourneyRequest request) {
        LearningJourneyResponse updated = journeyService.updateJourney(id, request);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Xóa hành trình học", description = "Xóa hành trình học (các bài đọc bên trong sẽ chuyển về trạng thái bài đọc độc lập)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteJourney(@PathVariable String id) {
        journeyService.deleteJourney(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa hành trình học ID: " + id));
    }
}
