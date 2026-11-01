package com.viecultures.backend.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viecultures.backend.dto.ReadingPostRequest;
import com.viecultures.backend.entity.LearningJourney;
import com.viecultures.backend.entity.User;
import com.viecultures.backend.repository.LearningJourneyRepository;
import com.viecultures.backend.repository.ReadingPostRepository;
import com.viecultures.backend.repository.UserRepository;
import com.viecultures.backend.service.ReadingPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ReadingPostRepository readingPostRepository;
    private final ReadingPostService readingPostService;
    private final LearningJourneyRepository journeyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) {
        initDefaultAdmin();
        seedLearningJourneys();
        seedReadingPosts();
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
