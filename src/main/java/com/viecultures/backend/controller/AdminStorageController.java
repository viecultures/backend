package com.viecultures.backend.controller;

import com.viecultures.backend.common.response.ApiResponse;
import com.viecultures.backend.common.annotation.AuditLog;
import com.viecultures.backend.dto.FileUploadResponse;
import com.viecultures.backend.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/storage")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Storage Management", description = "Dịch vụ lưu trữ tệp tin và hình ảnh lên Cloudflare R2 dành cho Quản trị viên")
public class AdminStorageController {

    private final StorageService storageService;

    @Operation(
            summary = "Tải tệp hình ảnh lên Cloudflare R2",
            description = "Tải lên hình ảnh (JPEG, PNG, WebP, AVIF, GIF, tối đa 5MB) lưu trữ trên Cloudflare R2 với CDN caching."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Tải lên thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Dung lượng hoặc định dạng tệp không hợp lệ"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Yêu cầu quyền Quản trị viên (ROLE_ADMIN)"
            )
    })
    @AuditLog(action = "UPLOAD_FILE", entity = "STORAGE", description = "Tải tệp hình ảnh lên Cloudflare R2")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileUploadResponse> uploadFile(
            @Parameter(description = "Tệp hình ảnh cần tải lên (tối đa 5MB)")
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Thư mục đích (ví dụ: journeys, avatars, posts, general)")
            @RequestParam(value = "folder", defaultValue = "general") String folder) {
        log.info("[AdminStorageController] Yêu cầu tải tệp lên: name='{}', size={} bytes, folder='{}'",
                file != null ? file.getOriginalFilename() : "null",
                file != null ? file.getSize() : 0,
                folder);

        FileUploadResponse response = storageService.uploadFile(file, folder);
        return ApiResponse.ok(response, "Tải tệp lên thành công");
    }
}
