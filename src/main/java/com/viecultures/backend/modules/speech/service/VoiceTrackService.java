package com.viecultures.backend.modules.speech.service;

import com.viecultures.backend.common.config.R2StorageConfig;
import com.viecultures.backend.modules.speech.dto.GenerateVoiceTrackRequest;
import com.viecultures.backend.modules.speech.dto.TtsRequest;
import com.viecultures.backend.modules.speech.dto.TtsResult;
import com.viecultures.backend.modules.speech.dto.VoiceModelDto;
import com.viecultures.backend.modules.speech.dto.VoiceTrackResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class VoiceTrackService {

    private final TtsManagerService ttsManagerService;
    private final R2StorageConfig r2Config;
    private final S3Client s3Client;

    @Value("${audio.output.dir:./uploads/audio}")
    private String audioOutputDir;

    public VoiceTrackService(
            TtsManagerService ttsManagerService,
            R2StorageConfig r2Config,
            @Autowired(required = false) S3Client s3Client) {
        this.ttsManagerService = ttsManagerService;
        this.r2Config = r2Config;
        this.s3Client = s3Client;
    }

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
            String normalUrl = saveAudioTrack("posts/" + normalFilename, normalResult.getAudioData());

            // 2. Tạo audio phiên bản Chậm (-15% cho người mới học / Shadowing)
            TtsResult slowResult = ttsManagerService.synthesize(TtsRequest.builder()
                    .text(text)
                    .voice(voiceId)
                    .speed("-15%")
                    .build());
            String slowUrl = saveAudioTrack("posts/" + slowFilename, slowResult.getAudioData());

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

    private String saveAudioTrack(String relativePath, byte[] data) throws Exception {
        if (s3Client != null && r2Config != null && r2Config.isConfigured()) {
            try {
                String cleanPath = relativePath.replace("\\", "/");
                String key = "audio/" + (cleanPath.startsWith("/") ? cleanPath.substring(1) : cleanPath);
                return uploadToR2(key, data, "audio/mpeg");
            } catch (Exception r2Ex) {
                log.warn("Lỗi khi tải file audio lên Cloudflare R2: {}. Chuyển sang lưu trữ tệp an toàn.", r2Ex.getMessage());
            }
        }
        return saveToFile(relativePath, data);
    }

    private String uploadToR2(String key, byte[] data, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(r2Config.getBucketName())
                .key(key)
                .contentType(contentType)
                .cacheControl("public, max-age=31536000, immutable")
                .build();
        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(data));
        String publicDomain = r2Config.getPublicDomain();
        if (publicDomain == null || publicDomain.trim().isEmpty()) {
            publicDomain = "https://cdn.viecultures.com";
        }
        publicDomain = publicDomain.trim();
        if (publicDomain.endsWith("/")) {
            publicDomain = publicDomain.substring(0, publicDomain.length() - 1);
        }
        String publicUrl = String.format("%s/%s", publicDomain, key);
        log.info("Đã tải tệp audio lên Cloudflare R2 thành công: {}", publicUrl);
        return publicUrl;
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

    private String saveToFile(String relativePath, byte[] data) throws Exception {
        Path targetPath;
        try {
            Path baseDir = Paths.get(audioOutputDir);
            if (!baseDir.isAbsolute()) {
                baseDir = Paths.get("").toAbsolutePath().resolve(baseDir).normalize();
            }
            // Phòng vệ đặc biệt: Nếu đường dẫn trỏ tới /frontend trên Linux root hoặc thư mục không có quyền ghi
            String baseStr = baseDir.toString().replace("\\", "/");
            if (baseStr.startsWith("/frontend") || !Files.isWritable(baseDir.getRoot() != null ? baseDir.getRoot() : Paths.get("."))) {
                baseDir = Paths.get(System.getProperty("java.io.tmpdir"), "viecultures-audio");
            }
            targetPath = baseDir.resolve(relativePath);
            if (targetPath.getParent() != null && !Files.exists(targetPath.getParent())) {
                Files.createDirectories(targetPath.getParent());
            }
        } catch (Exception ex) {
            log.warn("Không thể dùng đường dẫn lưu file {}: {}. Chuyển sang thư mục tạm hệ thống.", audioOutputDir, ex.getMessage());
            Path fallbackBase = Paths.get(System.getProperty("java.io.tmpdir"), "viecultures-audio");
            targetPath = fallbackBase.resolve(relativePath);
            if (targetPath.getParent() != null && !Files.exists(targetPath.getParent())) {
                Files.createDirectories(targetPath.getParent());
            }
        }

        try (FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
            fos.write(data);
        }

        return "/audio/" + relativePath.replace("\\", "/");
    }
}
