package com.viecultures.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viecultures.backend.dto.admin.AdminUserSummaryDto;
import com.viecultures.backend.dto.admin.UpdateUserRoleRequest;
import com.viecultures.backend.dto.admin.UpdateUserStatusRequest;
import com.viecultures.backend.entity.CefrLevel;
import com.viecultures.backend.service.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private AdminUserController adminUserController;

    private AdminUserSummaryDto sampleDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminUserController).build();
        objectMapper = new ObjectMapper();

        sampleDto = AdminUserSummaryDto.builder()
                .id("u-100")
                .name("Nguyen Thi C")
                .email("thic@example.com")
                .role("ROLE_USER")
                .authProvider("LOCAL")
                .enabled(true)
                .englishLevel(CefrLevel.B2)
                .createdAt(LocalDateTime.of(2026, 11, 2, 8, 0, 0))
                .savedVocabCount(25L)
                .build();
    }

    @Test
    @DisplayName("getUsers_withFilterGroup_returns200: Goi GET /api/v1/admin/users?filterGroup=recent_7d tra ve HTTP 200 kem data phan trang")
    void getUsers_withFilterGroup_returns200() throws Exception {
        Page<AdminUserSummaryDto> page = new PageImpl<>(List.of(sampleDto));
        when(adminUserService.getUsers(eq(null), eq(null), eq(null), eq("recent_7d"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("filterGroup", "recent_7d")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].id").value("u-100"))
                .andExpect(jsonPath("$.data.content[0].name").value("Nguyen Thi C"))
                .andExpect(jsonPath("$.data.content[0].email").value("thic@example.com"))
                .andExpect(jsonPath("$.data.content[0].role").value("ROLE_USER"))
                .andExpect(jsonPath("$.data.content[0].savedVocabCount").value(25));

        verify(adminUserService, times(1))
                .getUsers(eq(null), eq(null), eq(null), eq("recent_7d"), any(Pageable.class));
    }

    @Test
    @DisplayName("exportUsersCsv_returns200AndAttachmentHeader: Goi GET /api/v1/admin/users/export tra ve HTTP 200 kem header attachment va UTF-8 BOM")
    void exportUsersCsv_returns200AndAttachmentHeader() throws Exception {
        String csvContent = "\uFEFFID,Họ và tên,Email,Vai trò,Trình độ CEFR,Từ đã lưu,Phương thức đăng nhập,Ngày tạo,Trạng thái\r\n"
                + "u-100,\"Nguyen Thi C\",thic@example.com,Học viên,B2,25,LOCAL,02/11/2026 08:00:00,Đang hoạt động\r\n";
        byte[] expectedBytes = csvContent.getBytes(StandardCharsets.UTF_8);

        when(adminUserService.exportUsersCsv(eq("query"), eq("ROLE_USER"), eq(true), eq("active")))
                .thenReturn(expectedBytes);

        byte[] responseBytes = mockMvc.perform(get("/api/v1/admin/users/export")
                        .param("query", "query")
                        .param("role", "ROLE_USER")
                        .param("enabled", "true")
                        .param("filterGroup", "active"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"viecultures_users.csv\""))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertTrue(responseBytes.length >= 3);
        assertEquals((byte) 0xEF, responseBytes[0]);
        assertEquals((byte) 0xBB, responseBytes[1]);
        assertEquals((byte) 0xBF, responseBytes[2]);

        verify(adminUserService, times(1))
                .exportUsersCsv(eq("query"), eq("ROLE_USER"), eq(true), eq("active"));
    }

    @Test
    @DisplayName("exportUsersCsv_serviceError_returns500: Khi service nem RuntimeException, controller tra ve HTTP 500 voi envelope ApiResponse.error")
    void exportUsersCsv_serviceError_returns500() throws Exception {
        when(adminUserService.exportUsersCsv(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Simulated CSV exporter failure"));

        mockMvc.perform(get("/api/v1/admin/users/export"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("Simulated CSV exporter failure"));

        verify(adminUserService, times(1))
                .exportUsersCsv(any(), any(), any(), any());
    }

    @Test
    @DisplayName("getUserById_returns200: Lay chi tiet user theo ID tra ve HTTP 200 kem data hop le")
    void getUserById_returns200() throws Exception {
        when(adminUserService.getUserById("u-100")).thenReturn(sampleDto);

        mockMvc.perform(get("/api/v1/admin/users/u-100")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("u-100"))
                .andExpect(jsonPath("$.data.email").value("thic@example.com"));

        verify(adminUserService, times(1)).getUserById("u-100");
    }

    @Test
    @DisplayName("updateUserStatus_returns200: Cap nhat trang thai user tra ve HTTP 200")
    void updateUserStatus_returns200() throws Exception {
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(false);
        sampleDto.setEnabled(false);

        when(adminUserService.updateUserStatus(eq("u-100"), eq(false), any())).thenReturn(sampleDto);

        mockMvc.perform(patch("/api/v1/admin/users/u-100/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.message").value("Cập nhật trạng thái người dùng thành công"));

        verify(adminUserService, times(1)).updateUserStatus(eq("u-100"), eq(false), any());
    }

    @Test
    @DisplayName("updateUserRole_returns200: Cap nhat vai tro user tra ve HTTP 200")
    void updateUserRole_returns200() throws Exception {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest("ROLE_ADMIN");
        sampleDto.setRole("ROLE_ADMIN");

        when(adminUserService.updateUserRole(eq("u-100"), eq("ROLE_ADMIN"), any())).thenReturn(sampleDto);

        mockMvc.perform(patch("/api/v1/admin/users/u-100/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.message").value("Cập nhật vai trò người dùng thành công"));

        verify(adminUserService, times(1)).updateUserRole(eq("u-100"), eq("ROLE_ADMIN"), any());
    }

    @Test
    @DisplayName("updateUserRole_invalidArgument_returns400: Khi service nem IllegalArgumentException tra ve HTTP 400 voi INVALID_ARGUMENT")
    void updateUserRole_invalidArgument_returns400() throws Exception {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest("ROLE_ADMIN");

        when(adminUserService.updateUserRole(eq("u-100"), eq("ROLE_ADMIN"), any()))
                .thenThrow(new IllegalArgumentException("Vai trò không hợp lệ"));

        mockMvc.perform(patch("/api/v1/admin/users/u-100/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("Vai trò không hợp lệ"));
    }

    @Test
    @DisplayName("updateUserStatus_actionNotAllowed_returns400: Khi service nem IllegalStateException tra ve HTTP 400 voi ACTION_NOT_ALLOWED")
    void updateUserStatus_actionNotAllowed_returns400() throws Exception {
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(false);

        when(adminUserService.updateUserStatus(eq("u-100"), eq(false), any()))
                .thenThrow(new IllegalStateException("Bạn không thể tự khóa tài khoản quản trị của chính mình"));

        mockMvc.perform(patch("/api/v1/admin/users/u-100/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACTION_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("Bạn không thể tự khóa tài khoản quản trị của chính mình"));
    }
}
