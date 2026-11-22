package com.viecultures.backend.modules.admin.service;

import com.viecultures.backend.modules.admin.dto.AdminUserSummaryDto;
import com.viecultures.backend.entity.CefrLevel;
import com.viecultures.backend.entity.User;
import com.viecultures.backend.repository.UserRepository;
import com.viecultures.backend.repository.UserVocabProgressRepository;
import com.viecultures.backend.modules.admin.service.impl.AdminUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserVocabProgressRepository userVocabProgressRepository;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private User sampleUser1;
    private User sampleUser2;
    private User sampleAdmin;

    @BeforeEach
    void setUp() {
        sampleUser1 = User.builder()
                .id("user-1")
                .name("Nguyen Van A")
                .email("nguyenvana@example.com")
                .role("ROLE_USER")
                .authProvider("LOCAL")
                .enabled(true)
                .englishLevel(CefrLevel.B1)
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();

        sampleUser2 = User.builder()
                .id("user-2")
                .name("Tran Thi B")
                .email("tranthib@example.com")
                .role("ROLE_USER")
                .authProvider("GOOGLE")
                .enabled(false)
                .englishLevel(CefrLevel.A2)
                .createdAt(LocalDateTime.now().minusDays(15))
                .build();

        sampleAdmin = User.builder()
                .id("admin-1")
                .name("Admin Manager")
                .email("admin@viecultures.com")
                .role("ROLE_ADMIN")
                .authProvider("LOCAL")
                .enabled(true)
                .englishLevel(CefrLevel.C1)
                .createdAt(LocalDateTime.now().minusDays(30))
                .build();
    }

    @Test
    @DisplayName("getUsers_defaultAllGroup_callsSearchUsersPage: Goi searchUsers khi filterGroup null hoac all")
    void getUsers_defaultAllGroup_callsSearchUsersPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> mockPage = new PageImpl<>(List.of(sampleUser1, sampleUser2), pageable, 2);

        when(userRepository.searchUsers(eq("Nguyen"), eq("ROLE_USER"), eq(true), eq(pageable)))
                .thenReturn(mockPage);
        when(userVocabProgressRepository.countByUserId("user-1")).thenReturn(12L);
        when(userVocabProgressRepository.countByUserId("user-2")).thenReturn(0L);

        Page<AdminUserSummaryDto> result = adminUserService.getUsers("Nguyen", "ROLE_USER", true, "all", pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals("Nguyen Van A", result.getContent().get(0).getName());
        assertEquals(12L, result.getContent().get(0).getSavedVocabCount());
        verify(userRepository, times(1)).searchUsers("Nguyen", "ROLE_USER", true, pageable);
        verify(userRepository, never()).searchUsersList(any(), any(), any(), any());
    }

    @Test
    @DisplayName("getUsers_filterGroup_recent7d: Chi tra ve user duoc tao trong vong 7 ngay gan nhat")
    void getUsers_filterGroup_recent7d() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.searchUsersList(eq(null), eq(null), eq(null), any(Sort.class)))
                .thenReturn(List.of(sampleUser1, sampleUser2, sampleAdmin));
        when(userVocabProgressRepository.countByUserId(anyString())).thenReturn(5L);

        Page<AdminUserSummaryDto> result = adminUserService.getUsers(null, null, null, "recent_7d", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("user-1", result.getContent().get(0).getId());
        assertEquals("Nguyen Van A", result.getContent().get(0).getName());
        assertTrue(result.getContent().get(0).getCreatedAt().isAfter(LocalDateTime.now().minusDays(7)));
    }

    @Test
    @DisplayName("getUsers_filterGroup_admin: Chi tra ve user co role chua ADMIN")
    void getUsers_filterGroup_admin() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.searchUsersList(eq(null), eq(null), eq(null), any(Sort.class)))
                .thenReturn(List.of(sampleUser1, sampleUser2, sampleAdmin));
        when(userVocabProgressRepository.countByUserId(anyString())).thenReturn(2L);

        Page<AdminUserSummaryDto> result = adminUserService.getUsers(null, null, null, "admin", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("admin-1", result.getContent().get(0).getId());
        assertTrue(result.getContent().get(0).getRole().contains("ADMIN"));
    }

    @Test
    @DisplayName("getUsers_filterGroup_active: Loc dung user co savedVocabCount > 0 hoac enabled = true")
    void getUsers_filterGroup_active() {
        User disabledWithVocab = User.builder()
                .id("user-disabled-vocab")
                .name("Inactive But Saved Vocab")
                .email("vocab@test.com")
                .role("ROLE_USER")
                .enabled(false)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        User disabledZeroVocab = User.builder()
                .id("user-disabled-novocab")
                .name("Inactive Zero Vocab")
                .email("novocab@test.com")
                .role("ROLE_USER")
                .enabled(false)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.searchUsersList(eq(null), eq(null), eq(null), any(Sort.class)))
                .thenReturn(List.of(sampleUser1, disabledWithVocab, disabledZeroVocab));

        when(userVocabProgressRepository.countByUserId("user-1")).thenReturn(0L); // enabled = true -> active
        when(userVocabProgressRepository.countByUserId("user-disabled-vocab")).thenReturn(8L); // savedVocab > 0 -> active
        when(userVocabProgressRepository.countByUserId("user-disabled-novocab")).thenReturn(0L); // neither -> excluded

        Page<AdminUserSummaryDto> result = adminUserService.getUsers(null, null, null, "active", pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        List<String> userIds = result.getContent().stream().map(AdminUserSummaryDto::getId).toList();
        assertTrue(userIds.contains("user-1"));
        assertTrue(userIds.contains("user-disabled-vocab"));
        assertFalse(userIds.contains("user-disabled-novocab"));
    }

    @Test
    @DisplayName("getUsers_filterGroup_inactive: Loc dung user chua luu tu vung hoac bi disabled")
    void getUsers_filterGroup_inactive() {
        User activeWithVocab = User.builder()
                .id("user-fully-active")
                .name("Fully Active User")
                .email("fullyactive@test.com")
                .role("ROLE_USER")
                .enabled(true)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        User disabledWithVocab = User.builder()
                .id("user-disabled-vocab")
                .name("Disabled With Vocab")
                .email("disabled@test.com")
                .role("ROLE_USER")
                .enabled(false)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        User enabledZeroVocab = User.builder()
                .id("user-enabled-zerovocab")
                .name("Enabled Zero Vocab")
                .email("zerovocab@test.com")
                .role("ROLE_USER")
                .enabled(true)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.searchUsersList(eq(null), eq(null), eq(null), any(Sort.class)))
                .thenReturn(List.of(activeWithVocab, disabledWithVocab, enabledZeroVocab));

        when(userVocabProgressRepository.countByUserId("user-fully-active")).thenReturn(15L); // not inactive
        when(userVocabProgressRepository.countByUserId("user-disabled-vocab")).thenReturn(10L); // disabled -> inactive
        when(userVocabProgressRepository.countByUserId("user-enabled-zerovocab")).thenReturn(0L); // zero vocab -> inactive

        Page<AdminUserSummaryDto> result = adminUserService.getUsers(null, null, null, "inactive", pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        List<String> userIds = result.getContent().stream().map(AdminUserSummaryDto::getId).toList();
        assertTrue(userIds.contains("user-disabled-vocab"));
        assertTrue(userIds.contains("user-enabled-zerovocab"));
        assertFalse(userIds.contains("user-fully-active"));
    }

    @Test
    @DisplayName("exportUsersCsv_generatesValidCsvWithUtf8Bom: Tra ve mang byte bat dau bang UTF-8 BOM, header chuan va du lieu escape an toan")
    void exportUsersCsv_generatesValidCsvWithUtf8Bom() {
        User complexUser = User.builder()
                .id("user-special")
                .name("Nguyen, \"Van\" Special")
                .email("special@viecultures.com")
                .role("ROLE_ADMIN")
                .authProvider("LOCAL")
                .enabled(true)
                .englishLevel(CefrLevel.C2)
                .createdAt(LocalDateTime.of(2026, 11, 2, 8, 30, 0))
                .build();

        when(userRepository.searchUsersList(eq(null), eq(null), eq(null), any(Sort.class)))
                .thenReturn(List.of(complexUser));
        when(userVocabProgressRepository.countByUserId("user-special")).thenReturn(42L);

        byte[] csvBytes = adminUserService.exportUsersCsv(null, null, null, null);

        assertNotNull(csvBytes);
        assertTrue(csvBytes.length >= 3);

        // UTF-8 BOM byte sequence check: 0xEF, 0xBB, 0xBF
        assertEquals((byte) 0xEF, csvBytes[0]);
        assertEquals((byte) 0xBB, csvBytes[1]);
        assertEquals((byte) 0xBF, csvBytes[2]);

        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);

        // Kiem tra header chuan tieng Viet
        assertTrue(csvContent.contains("ID,Họ và tên,Email,Vai trò,Trình độ CEFR,Gói hội viên,Phương thức đăng nhập,Ngày tạo,Trạng thái"));

        // Kiem tra du lieu escape dau phay va ngoac kep: "Nguyen, ""Van"" Special"
        assertTrue(csvContent.contains("\"Nguyen, \"\"Van\"\" Special\""));
        assertTrue(csvContent.contains("special@viecultures.com"));
        assertTrue(csvContent.contains("Quản trị viên"));
        assertTrue(csvContent.contains("C2"));
        assertTrue(csvContent.contains("Hội Viên Pro (Admin)"));
        assertTrue(csvContent.contains("LOCAL"));
        assertTrue(csvContent.contains("Đang hoạt động"));
        assertTrue(csvContent.contains("02/11/2026 08:30:00"));
    }

    @Test
    @DisplayName("exportUsersCsv_emptyList_returnsHeaderOnly: Khi khong co user nao, CSV tra ve BOM kem header ma khong loi")
    void exportUsersCsv_emptyList_returnsHeaderOnly() {
        when(userRepository.searchUsersList(eq(null), eq(null), eq(null), any(Sort.class)))
                .thenReturn(Collections.emptyList());

        byte[] csvBytes = adminUserService.exportUsersCsv(null, null, null, null);

        assertNotNull(csvBytes);
        assertTrue(csvBytes.length >= 3);

        assertEquals((byte) 0xEF, csvBytes[0]);
        assertEquals((byte) 0xBB, csvBytes[1]);
        assertEquals((byte) 0xBF, csvBytes[2]);

        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);
        assertTrue(csvContent.contains("ID,Họ và tên,Email,Vai trò,Trình độ CEFR,Gói hội viên,Phương thức đăng nhập,Ngày tạo,Trạng thái"));

        // Chi co 1 dong header duy nhat (sau BOM)
        String contentWithoutBom = csvContent.substring(1).trim();
        String[] lines = contentWithoutBom.split("\r?\n");
        assertEquals(1, lines.length);
    }

    @Test
    @DisplayName("exportUsersCsv_serviceException_handledGracefully: Khi repository nem ngoai le, service bat loi an toan va tra ve fallback CSV")
    void exportUsersCsv_serviceException_handledGracefully() {
        when(userRepository.searchUsersList(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Database timeout error during CSV export"));

        byte[] fallbackBytes = adminUserService.exportUsersCsv(null, null, null, null);

        assertNotNull(fallbackBytes);
        assertTrue(fallbackBytes.length >= 3);

        assertEquals((byte) 0xEF, fallbackBytes[0]);
        assertEquals((byte) 0xBB, fallbackBytes[1]);
        assertEquals((byte) 0xBF, fallbackBytes[2]);

        String content = new String(fallbackBytes, StandardCharsets.UTF_8);
        assertTrue(content.contains("ID,Họ và tên,Email,Vai trò,Trình độ CEFR,Gói hội viên,Phương thức đăng nhập,Ngày tạo,Trạng thái"));
    }

    @Test
    @DisplayName("getUserById_found: Tra ve AdminUserSummaryDto hop le khi user ton tai")
    void getUserById_found() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sampleUser1));
        when(userVocabProgressRepository.countByUserId("user-1")).thenReturn(10L);

        AdminUserSummaryDto dto = adminUserService.getUserById("user-1");

        assertNotNull(dto);
        assertEquals("user-1", dto.getId());
        assertEquals("nguyenvana@example.com", dto.getEmail());
        assertEquals(10L, dto.getSavedVocabCount());
    }

    @Test
    @DisplayName("getUserById_notFound: Nem RuntimeException khi khong tim thay user")
    void getUserById_notFound() {
        when(userRepository.findById("unknown-id")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> adminUserService.getUserById("unknown-id"));

        assertTrue(exception.getMessage().contains("Người dùng không tồn tại"));
    }

    @Test
    @DisplayName("updateUserStatus_success: Cap nhat trang thai enable thanh cong")
    void updateUserStatus_success() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sampleUser1));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserSummaryDto updated = adminUserService.updateUserStatus("user-1", false, "admin@viecultures.com");

        assertNotNull(updated);
        assertFalse(updated.isEnabled());
        verify(userRepository, times(1)).save(sampleUser1);
    }

    @Test
    @DisplayName("updateUserStatus_adminCannotLockSelf: Chan admin tu khoa tai khoan cua chinh minh")
    void updateUserStatus_adminCannotLockSelf() {
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(sampleAdmin));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> adminUserService.updateUserStatus("admin-1", false, "admin@viecultures.com"));

        assertTrue(ex.getMessage().contains("Bạn không thể tự khóa tài khoản quản trị của chính mình"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUserRole_success: Cap nhat vai tro nguoi dung thanh cong")
    void updateUserRole_success() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sampleUser1));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserSummaryDto updated = adminUserService.updateUserRole("user-1", "ADMIN", "otheradmin@viecultures.com");

        assertNotNull(updated);
        assertEquals("ROLE_ADMIN", updated.getRole());
        verify(userRepository, times(1)).save(sampleUser1);
    }

    @Test
    @DisplayName("updateUserRole_adminCannotDemoteSelf: Chan admin tu ha quyen ROLE_ADMIN cua chinh minh")
    void updateUserRole_adminCannotDemoteSelf() {
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(sampleAdmin));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> adminUserService.updateUserRole("admin-1", "ROLE_USER", "admin@viecultures.com"));

        assertTrue(ex.getMessage().contains("Bạn không thể tự tước quyền Quản trị viên"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUserRole_invalidRole: Nem IllegalArgumentException khi role khong thuoc danh sach hop le")
    void updateUserRole_invalidRole() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sampleUser1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> adminUserService.updateUserRole("user-1", "SUPER_MODERATOR", "admin@viecultures.com"));

        assertTrue(ex.getMessage().contains("Vai trò không hợp lệ"));
        verify(userRepository, never()).save(any());
    }
}
