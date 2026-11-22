package com.viecultures.backend.modules.speech.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

@Slf4j
@Service
public class TtsCacheService {

    @Value("${tts.cache.dir}")
    private String ttsCacheDir;

    /**
     * Tạo hash MD5 từ sự kết hợp của: (text + voice + speed)
     */
    public String computeMd5(String text, String voice, String speed) {
        String cleanText = (text != null) ? text.trim() : "";
        String cleanVoice = (voice != null && !voice.trim().isEmpty()) ? voice.trim() : "en-US-JennyNeural";
        String cleanSpeed = (speed != null && !speed.trim().isEmpty()) ? speed.trim() : "0%";

        String payload = cleanText + "_" + cleanVoice + "_" + cleanSpeed;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Thuật toán MD5 không khả dụng trên môi trường JVM", e);
        }
    }

    /**
     * Xác định và tự động tạo thư mục cache trên đĩa cục bộ
     */
    public Path getCacheDirectory() {
        Path path = Paths.get(ttsCacheDir);
        if (!path.isAbsolute()) {
            path = Paths.get("").toAbsolutePath().resolve(path).normalize();
        }
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
                log.info("Initialized TTS cache directory: {}", path);
            } catch (IOException e) {
                log.error("Could not create TTS cache directory: {}", path, e);
            }
        }
        return path;
    }

    /**
     * Kiểm tra và đọc file âm thanh đã lưu đệm từ đĩa
     */
    public Optional<byte[]> getFromCache(String provider, String hash) {
        Path dir = getCacheDirectory();

        // 1. Kiểm tra file theo tên nhà cung cấp: <provider>_<hash>.mp3
        Path providerSpecificPath = dir.resolve(provider.toLowerCase() + "_" + hash + ".mp3");
        if (Files.exists(providerSpecificPath)) {
            try {
                log.info("TTS Cache HIT (provider file): {}", providerSpecificPath.getFileName());
                return Optional.of(Files.readAllBytes(providerSpecificPath));
            } catch (IOException e) {
                log.warn("Failed to read cached file: {}", providerSpecificPath, e);
            }
        }

        // 2. Kiểm tra file theo chuẩn MD5 thông thường: <hash>.mp3
        Path genericPath = dir.resolve(hash + ".mp3");
        if (Files.exists(genericPath)) {
            try {
                log.info("TTS Cache HIT (generic file): {}", genericPath.getFileName());
                return Optional.of(Files.readAllBytes(genericPath));
            } catch (IOException e) {
                log.warn("Failed to read cached file: {}", genericPath, e);
            }
        }

        // 3. Kiểm tra file định dạng .wav (dành cho Gemini PCM fallback)
        Path wavPath = dir.resolve(provider.toLowerCase() + "_" + hash + ".wav");
        if (Files.exists(wavPath)) {
            try {
                log.info("TTS Cache HIT (wav file): {}", wavPath.getFileName());
                return Optional.of(Files.readAllBytes(wavPath));
            } catch (IOException e) {
                log.warn("Failed to read cached file: {}", wavPath, e);
            }
        }

        return Optional.empty();
    }

    /**
     * Lưu file âm thanh mới vào thư mục cache
     */
    public void putCache(String provider, String hash, byte[] audioData, String extension) {
        if (audioData == null || audioData.length == 0) return;
        try {
            Path dir = getCacheDirectory();
            String ext = (extension != null && extension.startsWith(".")) ? extension : ".mp3";

            Path providerFile = dir.resolve(provider.toLowerCase() + "_" + hash + ext);
            Path genericFile = dir.resolve(hash + ext);

            Files.write(providerFile, audioData);
            Files.write(genericFile, audioData);

            log.info("TTS Cache SAVED: {} ({} bytes)", providerFile.getFileName(), audioData.length);
        } catch (IOException e) {
            log.warn("Failed to write to TTS audio cache: {}", e.getMessage());
        }
    }
}
