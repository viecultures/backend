package com.viecultures.backend.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viecultures.backend.dto.ReadingPostRequest;
import com.viecultures.backend.entity.LearningJourney;
import com.viecultures.backend.entity.SubscriptionPlan;
import com.viecultures.backend.entity.SubscriptionTransaction;
import com.viecultures.backend.entity.User;
import com.viecultures.backend.repository.LearningJourneyRepository;
import com.viecultures.backend.repository.ReadingPostRepository;
import com.viecultures.backend.repository.SubscriptionPlanRepository;
import com.viecultures.backend.repository.SubscriptionTransactionRepository;
import com.viecultures.backend.repository.UserRepository;
import com.viecultures.backend.service.ReadingPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ReadingPostRepository readingPostRepository;
    private final ReadingPostService readingPostService;
    private final LearningJourneyRepository journeyRepository;
    private final UserRepository userRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionTransactionRepository subscriptionTransactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) {
        initDefaultAdmin();
        seedLearningJourneys();
        seedReadingPosts();
        seedSubscriptionData();
    }

    private void initDefaultAdmin() {
        try {
            if (!userRepository.existsByEmail("admin@viecultures.com")) {
                User admin = User.builder()
                        .email("admin@viecultures.com")
                        .password(passwordEncoder.encode("123456"))
                        .name("Quan Tri Vien (Admin)")
                        .role("ROLE_ADMIN")
                        .authProvider("LOCAL")
                        .enabled(true)
                        .emailVerified(true)
                        .build();
                userRepository.save(admin);
                log.info("[DataInitializer] Da khoi tao tai khoan Admin mac dinh: admin@viecultures.com / 123456");
            }
        } catch (Exception e) {
            log.error("[DataInitializer] Loi khi khoi tao tai khoan Admin mac dinh: {}", e.getMessage(), e);
        }
    }

    private void seedLearningJourneys() {
        try {
            ClassPathResource resource = new ClassPathResource("seed-data/learning-journeys.json");
            if (!resource.exists()) {
                log.warn("[DataInitializer] Khong tim thay resource seed-data/learning-journeys.json");
                return;
            }

            try (InputStream is = resource.getInputStream()) {
                List<LearningJourneySeedItem> journeys = objectMapper.readValue(is, new TypeReference<List<LearningJourneySeedItem>>() {});
                int seededCount = 0;
                for (LearningJourneySeedItem item : journeys) {
                    if (!journeyRepository.existsById(item.getId())) {
                        LearningJourney journey = LearningJourney.builder()
                                .id(item.getId())
                                .titleEn(item.getTitleEn())
                                .titleVi(item.getTitleVi())
                                .theme(item.getTheme() != null ? item.getTheme().toLowerCase().trim() : "cuisine")
                                .provinceId(item.getProvinceId() != null ? item.getProvinceId().trim() : "")
                                .region(item.getRegion() != null ? item.getRegion().toLowerCase().trim() : "north")
                                .description(item.getDescription())
                                .imageUrl(item.getImageUrl())
                                .status(item.getStatus() != null ? item.getStatus().toLowerCase().trim() : "published")
                                .build();
                        journeyRepository.save(journey);
                        seededCount++;
                    }
                }
                log.info("[DataInitializer] Da hoan tat seed hanh trinh hoc. So luong moi them: {} / tong: {}", seededCount, journeys.size());
            }
        } catch (Exception e) {
            log.error("[DataInitializer] Loi khi nap du lieu seed-data/learning-journeys.json: {}", e.getMessage(), e);
        }
    }

    private void seedReadingPosts() {
        try {
            ClassPathResource resource = new ClassPathResource("seed-data/reading-posts.json");
            if (!resource.exists()) {
                log.warn("[DataInitializer] Khong tim thay resource seed-data/reading-posts.json");
                return;
            }

            try (InputStream is = resource.getInputStream()) {
                List<ReadingPostRequest> posts = objectMapper.readValue(is, new TypeReference<List<ReadingPostRequest>>() {});
                int seededCount = 0;
                for (ReadingPostRequest postReq : posts) {
                    if (postReq.getId() != null && readingPostRepository.existsById(postReq.getId())) {
                        continue;
                    }
                    try {
                        readingPostService.createPost(postReq);
                        seededCount++;
                    } catch (Exception e) {
                        log.error("[DataInitializer] Loi khi seed bai doc {}: {}", postReq.getId(), e.getMessage(), e);
                    }
                }
                log.info("[DataInitializer] Da hoan tat seed bai doc. So luong moi them: {} / tong: {}", seededCount, posts.size());
            }
        } catch (Exception e) {
            log.error("[DataInitializer] Loi khi nap du lieu seed-data/reading-posts.json: {}", e.getMessage(), e);
        }
    }

    private void seedSubscriptionData() {
        try {
            if (subscriptionPlanRepository.count() == 0) {
                SubscriptionPlan freePlan = SubscriptionPlan.builder()
                        .code("FREE")
                        .nameVi("Miễn phí trải nghiệm")
                        .nameEn("Cultural Explorer (Free)")
                        .descriptionVi("Dành cho người mới bắt đầu tìm hiểu văn hóa Việt Nam qua các bài đọc cơ bản")
                        .priceVnd(0L)
                        .discountPercent(0)
                        .billingCycle("MONTHLY")
                        .isActive(true)
                        .isPopularBadge(false)
                        .sortOrder(1)
                        .maxAiAudioMinutesPerMonth(15)
                        .maxCefrLevelAccess("B1")
                        .unlimitedJourneys(false)
                        .offlineDownloadsAllowed(false)
                        .highlightBadge(false)
                        .perksVi(new ArrayList<>(Arrays.asList(
                                "Truy cập bài đọc CEFR A1 - B1",
                                "15 phút nghe Audio Shadowing AI/tháng",
                                "Luyện tập từ vựng & Flashcards cơ bản"
                        )))
                        .perksEn(new ArrayList<>(Arrays.asList(
                                "Access CEFR A1 - B1 readings",
                                "15 mins AI Audio Shadowing/month",
                                "Basic Flashcards & Vocab practice"
                        )))
                        .build();

                SubscriptionPlan starterPlan = SubscriptionPlan.builder()
                        .code("STARTER")
                        .nameVi("Di sản Khởi hành")
                        .nameEn("Heritage Starter")
                        .descriptionVi("Mở rộng vốn từ vựng học thuật và phản xạ tiếng Anh qua các chủ đề địa phương")
                        .priceVnd(99000L)
                        .discountPercent(0)
                        .billingCycle("MONTHLY")
                        .isActive(true)
                        .isPopularBadge(false)
                        .sortOrder(2)
                        .maxAiAudioMinutesPerMonth(60)
                        .maxCefrLevelAccess("B2")
                        .unlimitedJourneys(true)
                        .offlineDownloadsAllowed(false)
                        .highlightBadge(false)
                        .perksVi(new ArrayList<>(Arrays.asList(
                                "Mở khóa bài đọc chuyên sâu tới CEFR B2",
                                "60 phút nghe giọng đọc AI Azure & Gemini",
                                "Truy cập toàn bộ bản đồ hành trình văn hóa",
                                "Theo dõi streak và nhật ký phản tư chuyên sâu"
                        )))
                        .perksEn(new ArrayList<>(Arrays.asList(
                                "Unlock readings up to CEFR B2",
                                "60 mins Azure & Gemini AI Voice",
                                "Full access to Cultural Journeys",
                                "Deep reflection logs & Streak tracking"
                        )))
                        .build();

                SubscriptionPlan proPlan = SubscriptionPlan.builder()
                        .code("PRO")
                        .nameVi("Di sản Toàn diện (Pro)")
                        .nameEn("Heritage Scholar (Pro)")
                        .descriptionVi("Lựa chọn phổ biến nhất dành cho người học chuyên sâu, du học sinh và giảng viên")
                        .priceVnd(199000L)
                        .discountPercent(15)
                        .billingCycle("MONTHLY")
                        .isActive(true)
                        .isPopularBadge(true)
                        .sortOrder(3)
                        .maxAiAudioMinutesPerMonth(180)
                        .maxCefrLevelAccess("C2")
                        .unlimitedJourneys(true)
                        .offlineDownloadsAllowed(true)
                        .highlightBadge(true)
                        .perksVi(new ArrayList<>(Arrays.asList(
                                "Mở khóa toàn bộ bài đọc học thuật C1 - C2",
                                "180 phút Audio AI giọng đọc tự nhiên",
                                "Tải tài liệu song ngữ & flashcards ngoại tuyến",
                                "AI Companion trò chuyện tương tác không giới hạn",
                                "Huy hiệu Học giả Di sản trên hồ sơ cá nhân"
                        )))
                        .perksEn(new ArrayList<>(Arrays.asList(
                                "Unlock all academic C1 - C2 readings",
                                "180 mins Natural AI Audio Voice",
                                "Offline downloads for bilingual texts & flashcards",
                                "Unlimited AI Companion conversations",
                                "Heritage Scholar Badge on profile"
                        )))
                        .build();

                SubscriptionPlan lifetimePlan = SubscriptionPlan.builder()
                        .code("LIFETIME")
                        .nameVi("Học giả Trọn đời")
                        .nameEn("Heritage Master (Lifetime)")
                        .descriptionVi("Đầu tư một lần, sở hữu vĩnh viễn toàn bộ kho tàng tri thức và tính năng tương lai")
                        .priceVnd(1490000L)
                        .discountPercent(25)
                        .billingCycle("ONE_TIME")
                        .isActive(true)
                        .isPopularBadge(false)
                        .sortOrder(4)
                        .maxAiAudioMinutesPerMonth(999)
                        .maxCefrLevelAccess("C2")
                        .unlimitedJourneys(true)
                        .offlineDownloadsAllowed(true)
                        .highlightBadge(true)
                        .perksVi(new ArrayList<>(Arrays.asList(
                                "Sở hữu trọn đời không cần gia hạn",
                                "Không giới hạn toàn bộ bài đọc và hành trình",
                                "Hạn ngạch AI Audio cao cấp tối đa",
                                "Ưu tiên quyền truy cập các tính năng AI mới sớm nhất",
                                "Huy hiệu Tinh hoa Di sản vĩnh viễn"
                        )))
                        .perksEn(new ArrayList<>(Arrays.asList(
                                "Lifetime access with no renewal required",
                                "Unlimited readings & cultural journeys",
                                "Premium AI Audio quota",
                                "Early priority access to new AI capabilities",
                                "Lifetime Heritage Elite Badge"
                        )))
                        .build();

                subscriptionPlanRepository.saveAll(Arrays.asList(freePlan, starterPlan, proPlan, lifetimePlan));
                log.info("[DataInitializer] Da seed thanh cong 4 goi dang ky mac dinh vao co so du lieu");
            }

            // Xoa toan bo cac giao dich gia lap mau cu (neu co) de co so du lieu 100% sach
            List<SubscriptionTransaction> testTxs = subscriptionTransactionRepository.findAll().stream()
                    .filter(t -> t.getTransactionCode() != null && t.getTransactionCode().startsWith("TX_HERITAGE_"))
                    .collect(java.util.stream.Collectors.toList());
            if (!testTxs.isEmpty()) {
                subscriptionTransactionRepository.deleteAll(testTxs);
                log.info("[DataInitializer] Da xoa toan bo {} giao dich gia lap khoi database", testTxs.size());
            }
        } catch (Exception e) {
            log.error("[DataInitializer] Loi khi khoi tao subscription plans: {}", e.getMessage(), e);
        }
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class LearningJourneySeedItem {
        private String id;
        private String titleEn;
        private String titleVi;
        private String theme;
        private String provinceId;
        private String region;
        private String description;
        private String imageUrl;
        private String status;
    }
}
