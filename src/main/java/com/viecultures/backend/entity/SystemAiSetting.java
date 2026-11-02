package com.viecultures.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_ai_settings", indexes = {
        @Index(name = "idx_sys_ai_setting_key", columnList = "setting_key", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemAiSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String settingKey;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String settingValue;

    @Column(name = "description", length = 255)
    private String description;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
