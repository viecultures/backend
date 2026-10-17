package com.viecultures.backend.service;

import com.viecultures.backend.dto.GenerateVoiceTrackRequest;
import com.viecultures.backend.dto.TtsRequest;
import com.viecultures.backend.dto.TtsResult;
import com.viecultures.backend.dto.VoiceModelDto;
import com.viecultures.backend.dto.VoiceTrackResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceTrackService {

    private final TtsManagerService ttsManagerService;

    @Value("${audio.output.dir}")
    private String audioOutputDir;

    private static final List<VoiceModelDto> CURATED_VOICE_MODELS = List.of(
            VoiceModelDto.builder()
                    .id("en-US-AmandaMultilingualNeural")
                    .name("Amanda (Nữ - Đa ngôn ngữ)")
                    .gender("Female")
                    .locale("en-US")
                    .description("Giọng Nữ chuẩn quốc tế, phát âm đa ngôn ngữ cực kỳ tự nhiên, truyền cảm và rõ ràng")
                    .isMultilingual(true)
                    .isPrimary(true)
                    .build(),

            VoiceModelDto.builder()
                    .id("en-US-DustinMultilingualNeural")
                    .name("Dustin (Nam - Đa ngôn ngữ)")
                    .gender("Male")
                    .locale("en-US")
                    .description("Giọng Nam trầm ấm, phong thái điềm đạm, hỗ trợ đa ngôn ngữ phát âm chuẩn xác")
                    .isMultilingual(true)
                    .isPrimary(true)
                    .build()
    );

    /**
     * Lấy danh sách các model giọng đọc được chọn lọc sẵn cho Admin lựa chọn
     */
    public List<VoiceModelDto> getAvailableModels() {
        return CURATED_VOICE_MODELS;
    }

    /**
     * Sinh 2 phiên bản tốc độ (Chậm -15% & Nhanh +15%) cho 1 bài viết theo model được chọn
     */
    public VoiceTrackResponse generateTrack(GenerateVoiceTrackRequest request) {
        String text = request.getText();
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung bài đọc tiếng Anh không được để trống");
        }

        String voiceId = (request.getVoiceId() != null && !request.getVoiceId().trim().isEmpty())
                ? request.getVoiceId().trim()
                : "en-US-AmandaMultilingualNeural";

        String voiceName = (request.getVoiceName() != null && !request.getVoiceName().trim().isEmpty())
                ? request.getVoiceName().trim()
                : findVoiceDisplayName(voiceId);

        String slug = (request.getTitleSlug() != null && !request.getTitleSlug().trim().isEmpty())
                ? sanitizeSlug(request.getTitleSlug())
                : "post_" + Math.abs(text.hashCode() % 100000);

        String shortVoice = voiceId.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();

        String slowFilename = slug + "_" + shortVoice + "_slow.mp3";
        String normalFilename = slug + "_" + shortVoice + "_normal.mp3";

        try {
            // 1. Tạo audio phiên bản Nguyên bản (Chuẩn 0% - Tốc độ mặc định)
            TtsResult normalResult = ttsManagerService.synthesize(TtsRequest.builder()
                    .text(text)
                    .voice(voiceId)
                    .speed("0%")
                    .build());
            saveToFile("posts/" + normalFilename, normalResult.getAudioData());

            // 2. Tạo audio phiên bản Chậm (-15% cho người mới học / Shadowing)
            TtsResult slowResult = ttsManagerService.synthesize(TtsRequest.builder()
                    .text(text)
                    .voice(voiceId)
                    .speed("-15%")
                    .build());
            saveToFile("posts/" + slowFilename, slowResult.getAudioData());

            String normalUrl = "/audio/posts/" + normalFilename;
            String slowUrl = "/audio/posts/" + slowFilename;

            log.info("Voice track generated successfully: voice={}, normal={}, slow={}", voiceId, normalUrl, slowUrl);

            return VoiceTrackResponse.builder()
                    .success(true)
                    .voiceId(voiceId)
                    .voiceName(voiceName)
                    .normalUrl(normalUrl)
                    .slowUrl(slowUrl)
                    .fastUrl(normalUrl) // alias tương thích ngược
                    .message("Tạo 2 phiên bản tốc độ (Nguyên bản mặc định & Chậm) thành công")
                    .createdAt(LocalDateTime.now())
                    .build();

        } catch (Exception ex) {
            log.error("Error generating voice track for voice {}: {}", voiceId, ex.getMessage(), ex);
            throw new RuntimeException("Không thể tạo giọng đọc cho model " + voiceName + ": " + ex.getMessage(), ex);
        }
    }

    private String findVoiceDisplayName(String voiceId) {
        return CURATED_VOICE_MODELS.stream()
                .filter(m -> m.getId().equalsIgnoreCase(voiceId))
                .map(VoiceModelDto::getName)
                .findFirst()
                .orElse(voiceId);
    }

    private String sanitizeSlug(String raw) {
        return raw.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9_-]", "_")
                .replaceAll("_+", "_");
    }

    private void saveToFile(String relativePath, byte[] data) throws Exception {
        Path baseDir = Paths.get(audioOutputDir);
        if (!baseDir.isAbsolute()) {
            baseDir = Paths.get("").toAbsolutePath().resolve(baseDir).normalize();
        }
        Path targetPath = baseDir.resolve(relativePath);
        if (targetPath.getParent() != null && !Files.exists(targetPath.getParent())) {
            Files.createDirectories(targetPath.getParent());
        }
        try (FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
            fos.write(data);
        }
    }
}
