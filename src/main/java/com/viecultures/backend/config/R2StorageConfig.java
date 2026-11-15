package com.viecultures.backend.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Slf4j
@Getter
@Configuration
public class R2StorageConfig {

    @Value("${cloudflare.r2.account-id:}")
    private String accountId;

    @Value("${cloudflare.r2.access-key-id:}")
    private String accessKeyId;

    @Value("${cloudflare.r2.secret-access-key:}")
    private String secretAccessKey;

    @Value("${cloudflare.r2.bucket-name:viecultures-assets}")
    private String bucketName;

    @Value("${cloudflare.r2.public-domain:}")
    private String publicDomain;

    public boolean isConfigured() {
        return accountId != null && !accountId.trim().isEmpty()
                && accessKeyId != null && !accessKeyId.trim().isEmpty()
                && secretAccessKey != null && !secretAccessKey.trim().isEmpty();
    }

    @Bean
    public S3Client s3Client() {
        if (!isConfigured()) {
            log.warn("Cloudflare R2 chua duoc cau hinh (thieu account-id, access-key-id hoac secret-access-key). He thong luu tru se hoat dong o che do mo phong fallback.");
            return null;
        }

        try {
            URI endpoint = URI.create(String.format("https://%s.r2.cloudflarestorage.com", accountId.trim()));
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId.trim(), secretAccessKey.trim());

            log.info("Khoi tao Cloudflare R2 S3Client thanh cong toi endpoint: {}", endpoint);
            return S3Client.builder()
                    .endpointOverride(endpoint)
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .region(Region.of("auto"))
                    .build();
        } catch (Exception e) {
            log.error("Khong the khoi tao Cloudflare R2 S3Client: {}", e.getMessage(), e);
            return null;
        }
    }
}
