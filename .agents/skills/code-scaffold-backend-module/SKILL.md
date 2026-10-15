---
name: code-scaffold-backend-module
description: Generates a complete Spring Boot 3.3.4 vertical slice (Entity, DTO, Repository, Service, Controller) adhering to VieCultures conventions.
---

# Scaffold Backend Module

Generates standard Spring Boot components for a new feature in `com.viecultures.backend`.

## Directives
1. **Directory Locations:**
   * Entity: `src/main/java/com/viecultures/backend/entity/<Feature>.java`
   * DTOs: `src/main/java/com/viecultures/backend/dto/<Feature>Dto.java`
   * Repository: `src/main/java/com/viecultures/backend/repository/<Feature>Repository.java`
   * Service: `src/main/java/com/viecultures/backend/service/<Feature>Service.java`
   * Controller: `src/main/java/com/viecultures/backend/controller/<Feature>Controller.java`

2. **Entity Standards:**
   ```java
   @Entity
   @Table(name = "feature_items", indexes = {
       @Index(name = "idx_feature_slug", columnList = "slug", unique = true)
   })
   @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
   public class Feature {
       @Id
       @GeneratedValue(strategy = GenerationType.IDENTITY)
       private Long id;

       @Column(nullable = false, unique = true)
       private String slug;

       @CreationTimestamp
       private LocalDateTime createdAt;

       @UpdateTimestamp
       private LocalDateTime updatedAt;
   }
   ```

3. **Controller Standards:**
   * Annotate with `@RestController`, `@RequestMapping("/api/v1/<features>")`, `@Tag(name = "<Feature>")`.
   * Return unified envelope: `ResponseEntity<ApiResponse<T>>`.
   * Include Swagger documentation `@Operation(summary = "...")`.
