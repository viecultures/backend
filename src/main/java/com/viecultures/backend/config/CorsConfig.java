package com.viecultures.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${audio.output.dir}")
    private String audioOutputDir;

    @Value("${storage.local.dir:uploads}")
    private String storageLocalDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        java.util.List<String> patterns = new java.util.ArrayList<>(java.util.Arrays.asList(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://*.vercel.app",
                "https://viecultures.vercel.app",
                "https://*.herokuapp.com"
        ));

        for (String origin : allowedOrigins.split(",")) {
            String trimmed = origin.trim();
            if (!trimmed.isEmpty() && !patterns.contains(trimmed)) {
                patterns.add(trimmed);
            }
        }

        registry.addMapping("/**")
                .allowedOriginPatterns(patterns.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .exposedHeaders("X-TTS-Provider", "X-TTS-Cached", "Content-Disposition", "Content-Type", "Content-Length")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path path = Paths.get(audioOutputDir);
        if (!path.isAbsolute()) {
            path = Paths.get("").toAbsolutePath().resolve(path).normalize();
        }
        String location = path.toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/audio/**")
                .addResourceLocations(location);
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(toDirectoryResourceLocation(storageLocalDir));
    }

    private String toDirectoryResourceLocation(String dir) {
        Path path = Paths.get(dir);
        if (!path.isAbsolute()) {
            path = Paths.get("").toAbsolutePath().resolve(path).normalize();
        }
        String loc = path.toUri().toString();
        return loc.endsWith("/") ? loc : loc + "/";
    }
}
