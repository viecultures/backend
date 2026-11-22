package com.viecultures.backend.controller;

import com.viecultures.backend.common.response.ApiResponse;
import com.viecultures.backend.common.annotation.AuditLog;
import com.viecultures.backend.dto.admin.ai.*;
import com.viecultures.backend.service.AdminAiConfigService;
import com.viecultures.backend.service.AdminAiPromptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/ai-configs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin AI Configuration", description = "Quan ly cau hinh cac khoa AI API va theo doi chi phi su dung danh cho Quan tri vien")
public class AdminAiConfigController {

    private final AdminAiConfigService adminAiConfigService;
    private final AdminAiPromptService adminAiPromptService;

    @Operation(summary = "Lay danh sach tat ca cau hinh khoa AI",
               description = "Tra ve danh sach cac khoa AI da ma hoa va an ky tu, sap xep theo nha cung cap va do uu tien.")
    @GetMapping({"", "/keys"})
    public ResponseEntity<ApiResponse<List<AiServiceConfigDto>>> getAllKeys() {
        List<AiServiceConfigDto> configs = adminAiConfigService.getAllConfigs();
        return ResponseEntity.ok(ApiResponse.ok(configs));
    }

    @Operation(summary = "Them cau hinh khoa AI moi",
               description = "Ma hoa AES-256 va luu tru khoa API cua Gemini hoac Azure Speech.")
    @AuditLog(action = "CREATE_AI_KEY", entity = "AI_CONFIG", description = "Tạo mới cấu hình khóa AI API")
    @PostMapping({"", "/keys"})
    public ResponseEntity<ApiResponse<AiServiceConfigDto>> createKey(
            @Valid @RequestBody CreateAiKeyRequest request) {
        AiServiceConfigDto created = adminAiConfigService.createConfig(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Tao cau hinh khoa AI thanh cong"));
    }

    @Operation(summary = "Cap nhat cau hinh khoa AI",
               description = "Cap nhat nhan khoa, gia tri API key moi, do uu tien hoac trang thai.")
    @AuditLog(action = "UPDATE_AI_KEY", entity = "AI_CONFIG", description = "Cập nhật thông tin cấu hình khóa AI API")
    @PutMapping("/keys/{id}")
    public ResponseEntity<ApiResponse<AiServiceConfigDto>> updateKey(
            @Parameter(description = "ID cau hinh can cap nhat")
            @PathVariable Long id,
            @RequestBody UpdateAiKeyRequest request) {
        AiServiceConfigDto updated = adminAiConfigService.updateConfig(id, request);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Cap nhat cau hinh khoa AI thanh cong"));
    }

    @Operation(summary = "Xoa cau hinh khoa AI",
               description = "Xoa bo cau hinh khoa khoi he thong.")
    @AuditLog(action = "DELETE_AI_KEY", entity = "AI_CONFIG", description = "Xóa bỏ cấu hình khóa AI API")
    @DeleteMapping("/keys/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteKey(
            @Parameter(description = "ID cau hinh can xoa")
            @PathVariable Long id) {
        adminAiConfigService.deleteConfig(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Xoa cau hinh khoa AI thanh cong"));
    }

    @Operation(summary = "Bat / tat trang thai kich hoat cua khoa AI",
               description = "Chuyen doi qua lai giua trang thai Active va Inactive cua khoa.")
    @AuditLog(action = "TOGGLE_AI_KEY", entity = "AI_CONFIG", description = "Chuyển đổi trạng thái kích hoạt khóa AI API")
    @PatchMapping("/keys/{id}/toggle")
    public ResponseEntity<ApiResponse<AiServiceConfigDto>> toggleKeyActive(
            @Parameter(description = "ID cau hinh can chuyen doi trang thai")
            @PathVariable Long id) {
        AiServiceConfigDto updated = adminAiConfigService.toggleActive(id);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Thay doi trang thai khoa thanh cong"));
    }

    @Operation(summary = "Kiem tra ket noi va tinh hop le cua khoa AI",
               description = "Gui yeu cau truc tiep den API Gemini hoac Azure de kiem tra tinh trang hoat dong va do tre cua khoa.")
    @PostMapping("/keys/test")
    public ResponseEntity<ApiResponse<TestAiKeyResponse>> testKey(
            @RequestBody TestAiKeyRequest request) {
        TestAiKeyResponse response = adminAiConfigService.testKeyConnection(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Lay cau hinh AI he thong va danh sach tuy chon kha dung",
               description = "Tra ve cau hinh model Gemini chat, translate, region Azure Speech va nha cung cap TTS hien tai.")
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<SystemAiSettingsDto>> getSystemSettings() {
        SystemAiSettingsDto settings = adminAiConfigService.getSystemSettings();
        return ResponseEntity.ok(ApiResponse.ok(settings));
    }

    @Operation(summary = "Cap nhat cau hinh he thong AI",
               description = "Luu tru cac thiet lap model Gemini, vung Azure Speech va TTS provider mac dinh.")
    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<SystemAiSettingsDto>> updateSystemSettings(
            @Valid @RequestBody UpdateSystemAiSettingsRequest request) {
        SystemAiSettingsDto updated = adminAiConfigService.updateSystemSettings(request);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Cap nhat cau hinh he thong AI thanh cong"));
    }

    @Operation(summary = "Thong ke tong hop chi phi va luot su dung AI",
               description = "Lay tong chi phi USD, chi phi hom nay, chi phi trong thang, phan bo theo nha cung cap va tinh nang.")
    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<AiCostMetricsDto>> getMetrics() {
        AiCostMetricsDto metrics = adminAiConfigService.getMetrics();
        return ResponseEntity.ok(ApiResponse.ok(metrics));
    }

    @Operation(summary = "Xem lich su nhat ky su dung AI phan trang",
               description = "Tra ve danh sach cac lan goi AI kem so luong token, so ky tu, chi phi uoc tinh va do tre.")
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Page<AiUsageLogDto>>> getUsageLogs(
            @Parameter(description = "Loc theo nha cung cap (GEMINI, AZURE)")
            @RequestParam(required = false) String provider,
            @Parameter(description = "Loc theo ma tinh nang (COMPANION_CHAT, TTS_READER, TRANSLATE)")
            @RequestParam(required = false) String featureCode,
            @Parameter(description = "So trang (bat dau tu 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "So ban ghi moi trang (toi da 100)")
            @RequestParam(defaultValue = "20") int size) {
        Page<AiUsageLogDto> logs = adminAiConfigService.getUsageLogs(provider, featureCode, page, size);
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }


    @Operation(summary = "Lay danh sach tat ca System Prompt AI",
               description = "Tra ve danh sach cac system prompt cua Companion Chat, Translation, STT va Ban do van hoa kem trang thai tuy chinh.")
    @GetMapping("/prompts")
    public ResponseEntity<ApiResponse<List<AiPromptConfigDto>>> getAllPrompts() {
        List<AiPromptConfigDto> prompts = adminAiPromptService.getAllPrompts();
        return ResponseEntity.ok(ApiResponse.ok(prompts));
    }

    @Operation(summary = "Lay chi tiet mot System Prompt AI",
               description = "Tra ve cau hinh prompt theo key dinh danh.")
    @GetMapping("/prompts/{key}")
    public ResponseEntity<ApiResponse<AiPromptConfigDto>> getPromptByKey(
            @Parameter(description = "Key dinh danh cua System Prompt")
            @PathVariable String key) {
        AiPromptConfigDto prompt = adminAiPromptService.getPromptByKey(key);
        return ResponseEntity.ok(ApiResponse.ok(prompt));
    }

    @Operation(summary = "Cap nhat System Prompt tuy chinh",
               description = "Luu noi dung prompt moi vao co so du lieu va kich hoat trang thai isModified = true.")
    @AuditLog(action = "UPDATE_AI_PROMPT", entity = "AI_PROMPT", description = "Cap nhat noi dung System Prompt AI")
    @PutMapping("/prompts/{key}")
    public ResponseEntity<ApiResponse<AiPromptConfigDto>> updatePrompt(
            @Parameter(description = "Key dinh danh cua System Prompt")
            @PathVariable String key,
            @Valid @RequestBody UpdateAiPromptRequest request) {
        AiPromptConfigDto updated = adminAiPromptService.updatePrompt(key, request);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Cap nhat System Prompt thanh cong"));
    }

    @Operation(summary = "Khoi phuc System Prompt ve mac dinh",
               description = "Xoa cau hinh tuy chinh trong co so du lieu va hoan nguyen ve prompt mac dinh ban dau.")
    @AuditLog(action = "RESET_AI_PROMPT", entity = "AI_PROMPT", description = "Khoi phuc System Prompt AI ve mac dinh")
    @PostMapping("/prompts/{key}/reset")
    public ResponseEntity<ApiResponse<AiPromptConfigDto>> resetPrompt(
            @Parameter(description = "Key dinh danh cua System Prompt")
            @PathVariable String key) {
        AiPromptConfigDto reset = adminAiPromptService.resetPrompt(key);
        return ResponseEntity.ok(ApiResponse.ok(reset, "Khoi phuc System Prompt ve mac dinh thanh cong"));
    }

}
