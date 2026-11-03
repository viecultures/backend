package com.viecultures.backend.service.impl;

import com.viecultures.backend.config.R2StorageConfig;
import com.viecultures.backend.dto.FileUploadResponse;
import com.viecultures.backend.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class R2StorageServiceImpl implements StorageService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/avif",
            "image/gif"
    );

    private final S3Client s3Client;
    private final R2StorageConfig r2Config;

    public R2StorageServiceImpl(@Autowired(required = false) S3Client s3Client, R2StorageConfig r2Config) {
        this.s3Client = s3Client;
        this.r2Config = r2Config;
    }

    @Override
    public FileUploadResponse uploadFile(MultipartFile file, String folder) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename() != null && !file.getOriginalFilename().trim().isEmpty()
                ? file.getOriginalFilename().trim()
                : "file";
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase().trim() : "image/jpeg";
        String ext = determineExtension(originalFilename, contentType);

        String cleanFolder = sanitizeFolder(folder);
        String key = generateStorageKey(cleanFolder, ext);
        String publicUrl = buildPublicUrl(key);

        if (s3Client == null || !r2Config.isConfigured()) {
            log.warn("Cloudflare R2 chua duoc cau hinh day du credentials. Dang su dung che do fallback mo phong URL cho tep: '{}'. URL: {}",
                    originalFilename, publicUrl);
            return FileUploadResponse.builder()
                    .url(publicUrl)
                    .key(key)
                    .fileName(originalFilename)
                    .size(file.getSize())
                    .contentType(contentType)
                    .build();
        }

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(r2Config.getBucketName())
                    .key(key)
                    .contentType(contentType)
                    .cacheControl("public, max-age=31536000, immutable")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("Tai len Cloudflare R2 thanh cong. Bucket: '{}', Key: '{}', Public URL: '{}'",
                    r2Config.getBucketName(), key, publicUrl);

            return FileUploadResponse.builder()
                    .url(publicUrl)
                    .key(key)
                    .fileName(originalFilename)
                    .size(file.getSize())
                    .contentType(contentType)
                    .build();
        } catch (IOException e) {
            log.error("Loi doc du lieu tep khi tai len Cloudflare R2: {}", e.getMessage(), e);
            throw new RuntimeException("Khong the doc noi dung tep: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Loi khi tai tep len Cloudflare R2: {}", e.getMessage(), e);
            throw new RuntimeException("Khong the tai tep len Cloudflare R2: " + e.getMessage(), e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Tep tai len khong duoc de trong");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(String.format(
                    "Dung luong tep vuot qua gioi han cho phep (%d bytes > %d bytes toi da 5MB)",
                    file.getSize(), MAX_FILE_SIZE_BYTES));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase().trim())) {
            throw new IllegalArgumentException(String.format(
                    "Dinh dang tep khong hop le: '%s'. Chi chap nhan cac dinh dang: %s",
                    contentType, ALLOWED_CONTENT_TYPES));
        }
    }

    private String sanitizeFolder(String folder) {
        if (folder == null || folder.trim().isEmpty()) {
            return "general";
        }
        String cleaned = folder.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "");
        return cleaned.isEmpty() ? "general" : cleaned;
    }

    private String generateStorageKey(String cleanFolder, String ext) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        String year = now.format(DateTimeFormatter.ofPattern("yyyy"));
        String month = now.format(DateTimeFormatter.ofPattern("MM"));
        long timestamp = Instant.now().getEpochSecond();
        String uuid8 = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        return String.format("%s/%s/%s/%s-%d-%s.%s", cleanFolder, year, month, cleanFolder, timestamp, uuid8, ext);
    }

    private String determineExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            String candidate = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).trim().toLowerCase();
            if (Set.of("jpg", "jpeg", "png", "webp", "avif", "gif").contains(candidate)) {
                return candidate.equals("jpeg") ? "jpg" : candidate;
            }
        }

        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/avif" -> "avif";
            case "image/gif" -> "gif";
            default -> "webp";
        };
    }

    private String buildPublicUrl(String key) {
        String publicDomain = r2Config.getPublicDomain();
        if (publicDomain == null || publicDomain.trim().isEmpty()) {
            publicDomain = "https://cdn.viecultures.com";
        }
        publicDomain = publicDomain.trim();
        if (publicDomain.endsWith("/")) {
            publicDomain = publicDomain.substring(0, publicDomain.length() - 1);
        }
        return String.format("%s/%s", publicDomain, key);
    }
}
