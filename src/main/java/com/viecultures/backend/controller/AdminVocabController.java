package com.viecultures.backend.controller;

import com.viecultures.backend.dto.ApiResponse;
import com.viecultures.backend.dto.DictionaryLookupDto;
import com.viecultures.backend.dto.admin.AdminVocabRequest;
import com.viecultures.backend.service.AdminVocabService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/vocabularies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "6. Admin Vocabulary Management", description = "Quan ly tu dien, tu vung va phan loai trinh do danh cho Quan tri vien")
public class AdminVocabController {

    private final AdminVocabService adminVocabService;

    @Operation(summary = "Lay danh sach tu vung phan trang & loc",
               description = "Ho tro tim kiem theo tu khoa (word, viMeaning), loc theo cap do (A1, A2, B1, B2, C1, C2) va phan trang.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<DictionaryLookupDto>>> getVocabularies(
            @Parameter(description = "Tu khoa tim kiem theo tu tieng Anh hoac nghia tieng Viet")
            @RequestParam(required = false) String search,
            @Parameter(description = "Loc theo cap do CEFR (A1, A2, B1, B2, C1, C2 hoac ALL)")
            @RequestParam(required = false) String level,
            @Parameter(description = "So trang, bat dau tu 0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "So luong ban ghi tren moi trang (1-100)")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Truong sap xep (word, searchCount, createdAt, level)")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Thu tu sap xep (asc hoac desc)")
            @RequestParam(defaultValue = "desc") String direction) {

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String safeSortBy = "word".equalsIgnoreCase(sortBy) || "searchCount".equalsIgnoreCase(sortBy) || "level".equalsIgnoreCase(sortBy)
                ? sortBy
                : "createdAt";

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(sortDirection, safeSortBy));
        Page<DictionaryLookupDto> result = adminVocabService.getAdminVocabularies(search, level, pageable);

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "Lay chi tiet tu vung theo ID",
               description = "Tra ve toan bo thong tin chi tiet cua mot tu vung trong CSDL theo ID.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DictionaryLookupDto>> getVocabularyById(
            @Parameter(description = "ID cua tu vung can lay thong tin")
            @PathVariable String id) {
        DictionaryLookupDto result = adminVocabService.getVocabularyById(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "Them moi hoac upsert tu vung",
               description = "Them moi tu vung hoac tu dong cap nhat neu tu vung da ton tai trong CSDL.")
    @PostMapping
    public ResponseEntity<ApiResponse<DictionaryLookupDto>> createVocabulary(
            @Valid @RequestBody AdminVocabRequest request) {
        log.info("[AdminVocabController] Yeu cau them moi hoac upsert tu vung: '{}'", request.getWord());
        DictionaryLookupDto created = adminVocabService.createVocabulary(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Luu tu vung thanh cong"));
    }

    @Operation(summary = "Cap nhat thong tin tu vung theo ID",
               description = "Cap nhat nghia, phien am, vi du ngu canh, trinh do va am thanh cua tu vung.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DictionaryLookupDto>> updateVocabulary(
            @Parameter(description = "ID cua tu vung can cap nhat")
            @PathVariable String id,
            @Valid @RequestBody AdminVocabRequest request) {
        log.info("[AdminVocabController] Yeu cau cap nhat tu vung id='{}'", id);
        DictionaryLookupDto updated = adminVocabService.updateVocabulary(id, request);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Cap nhat tu vung thanh cong"));
    }

    @Operation(summary = "Xoa tu vung khoi he thong theo ID",
               description = "Xoa hoan toan ban ghi tu vung khoi he thong tu dien.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVocabulary(
            @Parameter(description = "ID cua tu vung can xoa")
            @PathVariable String id) {
        log.info("[AdminVocabController] Yeu cau xoa tu vung id='{}'", id);
        adminVocabService.deleteVocabulary(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Xoa tu vung thanh cong"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("[AdminVocabController] Yeu cau khong hop le: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), "INVALID_ARGUMENT"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        log.warn("[AdminVocabController] Thao tac khong duoc phep: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), "ACTION_NOT_ALLOWED"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("[AdminVocabController] Loi he thong khi xu ly tu vung: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Loi may chu noi bo: " + ex.getMessage(), "INTERNAL_SERVER_ERROR"));
    }
}
