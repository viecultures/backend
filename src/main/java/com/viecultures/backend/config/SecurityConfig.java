package com.viecultures.backend.config;

import com.viecultures.backend.security.RestAuthenticationEntryPoint;
import com.viecultures.backend.security.jwt.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        // Cho phép toàn bộ pre-flight OPTIONS requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Endpoints xác thực công khai & OTP Email
                        .requestMatchers("/api/v1/auth/**", "/api/auth/**").permitAll()
                        .requestMatchers("/api/v1/mail/**").permitAll()

                        // Endpoints đọc bài & học tập công khai (Catalog, Chi tiết bài đọc, Đếm lượt đọc)
                        .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/posts/*/read").permitAll()

                        // Vocabulary & Dictionary Lookups & Progress Persistence
                        .requestMatchers(HttpMethod.GET, "/api/vocabularies/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/vocabularies/progress").permitAll()
                        .requestMatchers("/api/user_vocab_progress/**").permitAll()

                        // Audio & TTS Service
                        .requestMatchers("/api/tts/**").permitAll()
                        .requestMatchers("/api/voice/**").permitAll()
                        .requestMatchers("/audio/**").permitAll()

                        // AI Companion Chat Service (Public with internal rate-limiting)
                        .requestMatchers("/api/v1/companion/**").permitAll()

                        // System Health
                        .requestMatchers("/api/health/**").permitAll()

                        // Swagger / OpenAPI documentation
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/error").permitAll()

                        // Endpoints Quản trị viên (Admin Management & Metrics)
                        .requestMatchers("/api/v1/admin/**", "/api/admin/**").hasRole("ADMIN")

                        // Các API còn lại (tạo bài viết, lưu tiến độ học, reflection cá nhân) yêu cầu đăng nhập
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
