package com.viecultures.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("VieCultures Backend API")
                        .version("v1.0")
                        .description("Tài liệu API chính thức cho nền tảng học tiếng Anh qua văn hóa Việt Nam (VieCultures).\n" +
                                "- Hỗ trợ bài đọc song ngữ & âm thanh AI Shadowing\n" +
                                "- Từ vựng học thuật theo ngữ cảnh\n" +
                                "- Ôn tập Flashcards (Spaced Repetition)\n" +
                                "- Sinh giọng nói tự nhiên với Google Gemini TTS")
                        .contact(new Contact()
                                .name("VieCultures Team")
                                .email("contact@viecultures.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://springdoc.org")))
                .servers(List.of(
                        new Server().url("/").description("Current Server (Local / Render)")
                ));
    }
}
