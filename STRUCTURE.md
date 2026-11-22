# VieCultures Backend - Agent Architecture & Execution Guide

> **Target Audience:** AI Agent / Code Reviewer / Developer  
> **Primary Purpose:** Instant context priming, reusable roadmap for creating new APIs, and code review audit checklist without re-exploring the codebase.  
> **Project Scope:** `backend` (Spring Boot 3.3.4 REST API & AI/TTS Service)

---

## 1. System Specifications & Agent Directives

### 1.1 Tech Stack & Runtime
- **Java Version:** `17`
- **Framework:** `Spring Boot 3.3.4` (`org.springframework.boot:spring-boot-starter-web`, `data-jpa`)
- **Database:** PostgreSQL (`org.postgresql:postgresql`) via Supabase Pooler (`jdbc:postgresql://...`)
- **ORM / Persistence:** Hibernate 6 / Spring Data JPA (`spring.jpa.hibernate.ddl-auto=update`, `open-in-view=false`)
- **Boilerplate Reduction:** Project Lombok (`@Getter`, `@Setter`, `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor`)
- **API Documentation:** Springdoc OpenAPI 2.6.0 (`/swagger-ui.html`, `/v3/api-docs`)
- **External Integrations:**
  - **Azure Cognitive Speech REST API:** Primary TTS provider (`tts.provider=azure`, 500k chars/mo quota).
  - **Google Gemini API (`gemini-3.8-flash`):** AI Voice synthesis & fallback audio provider.
  - **Audio Cache:** File-system caching using MD5 hashes (`audio-cache/`) and static serving via `/audio/**`.
- **Default Port:** `8080` (or injected via `${PORT}`)

### 1.2 Invariant Architectural Rules (Must Follow)
1. **Layer Separation:** `Controller -> Service -> Repository -> Database`.
   - Controllers **must never** inject Repositories directly.
   - All business logic, transaction handling, and Entity-DTO mapping reside strictly in `service/`.
2. **DTO Isolation:**
   - **Never return JPA Entities directly in Controller responses**. Entities contain proxy references and `@JsonIgnore` cycles that will crash serialization or cause N+1 query leaks.
   - Always map Entity <-> DTO in the Service layer (or mapper).
3. **Lombok + JPA Circular Reference Prevention:**
   - On `@OneToMany` and `@ManyToOne` relationships, **always** add `@ToString.Exclude` and `@EqualsAndHashCode.Exclude` (or avoid `@Data` on complex bidirectional entities).
   - Bidirectional references without `@ToString.Exclude` cause fatal `StackOverflowError` during logging or debugging.
4. **Transaction Boundaries:**
   - Queries: `@Transactional(readOnly = true)`.
   - CUD Operations (Create, Update, Delete): `@Transactional`.
5. **OpenAPI / Swagger Standardization:**
   - Every Controller must have `@Tag(name = "...", description = "...")`.
   - Every endpoint method must have `@Operation(summary = "...", description = "...")`.

---

## 2. Directory & Package Topology

Root package: `com.viecultures.backend` (under `backend/src/main/java/com/viecultures/backend/`)

```
backend/
├── pom.xml                                    # Maven dependencies & build plugins
├── .env / .env.example                        # Environment variables (port, db, keys)
├── audio-cache/                               # Local MD5 audio cache directory
└── src/
    ├── main/
    │   ├── java/com/viecultures/backend/
    │   │   ├── VieculturesBackendApplication.java  # Spring Boot Main Entry Point
    │   │   │
    │   │   ├── common/                        # Common Infrastructure, Shared Config & Security
    │   │   │   ├── config/                    # Cross-cutting & Infrastructure Configurations
    │   │   │   │   ├── AppConfig.java         # General beans (RestTemplate, etc.)
    │   │   │   │   ├── AsyncConfig.java       # @EnableAsync & ThreadPoolTaskExecutor
    │   │   │   │   ├── CacheConfig.java       # In-memory Caffeine Caching (@EnableCaching)
    │   │   │   │   ├── CorsConfig.java        # WebMvcConfigurer: CORS & /audio/** static resource mapping
    │   │   │   │   ├── OpenApiConfig.java     # OpenAPI 3.0 / Swagger metadata + JWT Bearer scheme config
    │   │   │   │   ├── R2StorageConfig.java   # Cloudflare R2 / AWS S3 client configuration
    │   │   │   │   └── SecurityConfig.java    # Spring Security: Stateless JWT filter chain, public/auth routes
    │   │   │   │
    │   │   │   ├── security/                  # Spring Security & JWT Implementation
    │   │   │   │   ├── CustomUserDetailsService.java # UserDetailsService loading from UserRepository
    │   │   │   │   ├── RestAuthenticationEntryPoint.java # 401 Unauthorized JSON Handler
    │   │   │   │   ├── JwtAuthFilter.java     # OncePerRequestFilter checking Bearer header
    │   │   │   │   └── JwtService.java        # JJWT HMAC-SHA256 signing, claims & validation
    │   │   │   │
    │   │   │   ├── response/                  # Standard Response Wrappers
    │   │   │   │   └── ApiResponse.java       # Unified JSON response envelope (success, data, message, errorCode)
    │   │   │   │
    │   │   │   ├── exception/                 # Centralized Exception Handling & Standard Error Codes
    │   │   │   │   ├── ErrorCode.java         # Standard Error Codes interface/enum
    │   │   │   │   ├── ApiException.java      # Base business RuntimeException
    │   │   │   │   ├── TtsException.java      # Runtime exception for TTS/Azure/Gemini failures
    │   │   │   │   └── GlobalExceptionHandler.java # @RestControllerAdvice centralized error interceptor
    │   │   │   │
    │   │   │   ├── annotation/                # Cross-cutting Custom Annotations
    │   │   │   │   └── AuditLog.java          # Marker annotation for admin audit logging
    │   │   │   │
    │   │   │   └── aspect/                    # Aspect-Oriented Programming (AOP) Aspects
    │   │   │       └── AdminAuditAspect.java  # Admin audit logging interceptor
    │   │   │
    │   │   ├── bootstrap/                     # Application Startup & Data Seeding
    │   │   │   └── DataInitializer.java       # CommandLineRunner: Seeds sample posts on empty DB
    │   │   │
    │   │   ├── integration/                   # Third-Party External Clients & Integrations
    │   │   │   ├── azure/                     # Azure Speech REST SSML Client (AzureSpeechService)
    │   │   │   ├── gemini/                    # Gemini AI Voice & Dynamic Key Manager (GeminiVoiceService, DynamicApiKeyService)
    │   │   │   ├── google/                    # Google OAuth2 & API Integration (GoogleAuthService, GoogleIntegrationService)
    │   │   │   ├── mail/                      # SMTP & Mail Dispatcher (EmailService)
    │   │   │   └── storage/                   # Cloudflare R2 Object Storage (StorageService, R2StorageServiceImpl)
    │   │   │
    │   │   ├── modules/                       # Feature-First Modular Slices (Domain Modules)
    │   │   │   ├── speech/                    # TTS, STT & Voice Track Audio Processing
    │   │   │   │   ├── controller/            # TtsController, VoiceController, TwoPassSttController
    │   │   │   │   ├── service/               # TtsManagerService, VoiceTrackService, TwoPassSttService, TtsCacheService
    │   │   │   │   └── dto/                   # TtsRequest, TtsResult, VoiceTrackResponse, etc.
    │   │   │   │
    │   │   │   └── auth/                      # Authentication & Account Security
    │   │   │       ├── controller/            # AuthController
    │   │   │       ├── service/               # AuthService, OtpService, AuthServiceImpl
    │   │   │       └── dto/                   # AuthRequest, AuthResponse, RegisterRequest, etc.
    │   │   │
    │   │   ├── controller/                    # REST API Endpoints (@RestController)
    │   │   │   ├── AuthController.java        # /api/v1/auth (Login, Register, Google OAuth, Me)
    │   │   │   ├── HealthController.java      # GET /api/health
    │   │   │   ├── ReadingPostController.java # /api/posts (Catalog, CRUD, Read count)
    │   │   │   ├── TtsController.java          # POST /api/tts (Audio stream, MD5 cache, fallback)
    │   │   │   └── VoiceController.java       # /api/voice (Models, multi-speed track generation)
    │   │   │
    │   │   ├── dto/                           # Data Transfer Objects (Requests & Responses)
    │   │   │   ├── ApiResponse.java           # Unified JSON response envelope (success, data, message, errorCode)
    │   │   │   ├── AuthRequest.java           # Login request (email, password)
    │   │   │   ├── AuthResponse.java          # Auth result envelope (accessToken, expiresIn, UserInfo)
    │   │   │   ├── CatalogLessonDto.java      # Grouped multi-level reading post DTO
    │   │   │   ├── CatalogResponse.java       # Catalog pagination + topic counters envelope
    │   │   │   ├── GenerateVoiceTrackRequest.java # Voice track generation payload
    │   │   │   ├── GoogleLoginRequest.java    # Google OAuth ID Token / Profile payload
    │   │   │   ├── ReadingPostRequest.java    # Create/update post request (nested paragraphs/vocab)
    │   │   │   ├── ReadingPostResponse.java   # Post detail & summary response
    │   │   │   ├── RegisterRequest.java       # Learner registration payload
    │   │   │   ├── TtsErrorResponse.java      # Standard TTS error envelope
    │   │   │   ├── TtsRequest.java            # TTS synthesis request payload
    │   │   │   ├── TtsResult.java             # Internal TTS byte array result + headers
    │   │   │   ├── VoiceGenerateRequest.java  # Legacy Gemini voice request
    │   │   │   ├── VoiceGenerateResponse.java # Legacy Gemini voice response
    │   │   │   ├── VoiceModelDto.java         # Voice actor profile DTO
    │   │   │   └── VoiceTrackResponse.java    # Slow/Fast audio URLs response
    │   │   │
    │   │   ├── entity/                        # JPA Database Entities
    │   │   │   ├── CefrLevel.java             # Enum: A1, A2, B1, B2, C1, C2
    │   │   │   ├── ParagraphPair.java         # Bilingual paragraph entity (en_text, vi_text, timestamps)
    │   │   │   ├── ReadingPost.java           # Reading article root aggregate entity
    │   │   │   ├── ReflectionStatus.java      # Enum: DRAFT, PUBLISHED, ARCHIVED
    │   │   │   ├── User.java                  # User profile entity (implements UserDetails)
    │   │   │   ├── UserReflection.java        # Learner reflection & vocab usage entity
    │   │   │   ├── UserVocabProgress.java     # User vocabulary mastery progress (Composite ID)
    │   │   │   ├── VocabItem.java             # Vocabulary item (word, ipa, pos, vi_meaning, context)
    │   │   │   └── VocabProgressStatus.java   # Enum: NEW, LEARNING, MASTERED
    │   │   │
    │   │   ├── repository/                    # Spring Data JPA Interfaces
    │   │   │   ├── ParagraphPairRepository.java
    │   │   │   ├── ReadingPostRepository.java # JpaSpecificationExecutor for multi-filter search
    │   │   │   ├── UserReflectionRepository.java
    │   │   │   ├── UserRepository.java        # findByEmail, findByGoogleId, existsByEmail
    │   │   │   ├── UserVocabProgressRepository.java
    │   │   │   └── VocabItemRepository.java
    │   │   │
    │   │   └── service/                       # Business Logic & External API Integrations
    │   │       ├── AuthService.java           # Authentication service interface
    │   │       ├── AuthServiceImpl.java       # Local Auth & Google OAuth token verification
    │   │       ├── AzureSpeechService.java    # Azure REST SSML TTS client
    │   │       ├── GeminiVoiceService.java    # Google Gemini AI Speech synthesis client
    │   │       ├── ReadingPostService.java    # Reading catalog, filtering, and CRUD operations
    │   │       ├── TtsCacheService.java       # MD5 hashing & disk audio cache manager
    │   │       ├── TtsManagerService.java     # Provider dispatcher & automatic failover engine
    │   │       └── VoiceTrackService.java     # Article audio track generator (slow -15% & fast +15%)
    │   │
    │   └── resources/
    │       └── application.properties         # DB, HikariCP, JPA, TTS, CORS, JWT config
    │
    └── test/java/com/viecultures/backend/
        ├── VieculturesBackendApplicationTests.java
        ├── controller/TtsControllerTest.java  # Mockito unit tests for validation & controller responses
        └── service/TtsServiceTest.java        # Unit tests for TTS provider routing & cache
```

---

## 3. Database Schema & Domain Relationships

```
              ┌────────────────────────┐
              │          User          │
              │ id: String (UUID)      │
              │ email: String (Unique) │
              └───────────┬────────────┘
                          │ 1
                          │
          ┌───────────────┴───────────────┐
          │ N                             │ N
┌─────────▼──────────────┐      ┌─────────▼───────────────────────────┐
│     UserReflection     │      │          UserVocabProgress          │
│ id: String (UUID)      │      │ id: EmbeddedId(userId, vocabId)     │
│ content: TEXT          │      │ status: Enum(NEW, LEARNING, MASTERED│
│ status: Enum           │      │ reviewCount: Integer                │
│ post_id: FK            │      │ lastReviewedAt: LocalDateTime       │
└─────────▲──────────────┘      └─────────▲───────────────────────────┘
          │ N                             │ N (MapsId vocabId)
          │                               │
          │ 1                             │ 1
┌─────────┴──────────────┐      ┌─────────┴───────────────────────────┐
│      ReadingPost       │ 1  N │              VocabItem              │
│ id: String (UUID)      ├──────► id: String (UUID)                   │
│ topicGroup: String     │      │ word: String                        │
│ titleEn / titleVi      │      │ ipa / pos / viMeaning / context     │
│ level: Enum(CefrLevel) │      │ level: Enum(CefrLevel)              │
│ totalReads: Integer    │      └─────────────────────────────────────┘
└─────────┬──────────────┘
          │ 1
          │ N
┌─────────▼──────────────┐
│     ParagraphPair      │
│ id: String (UUID)      │
│ orderIndex: Integer    │
│ enText / viText: TEXT  │
│ audioTimestampStart/End│
└────────────────────────┘
```

### Key Schema Characteristics
- **Primary Keys:** Most entities use `@GeneratedValue(strategy = GenerationType.UUID)` string IDs.
- **Composite Key:** `UserVocabProgress` uses `@EmbeddedId` (`UserVocabProgressId` with `userId` and `vocabId`).
- **Cascade Strategy:** `ReadingPost` cascades `ALL` with `orphanRemoval = true` to `ParagraphPair`, `VocabItem`, and `UserReflection`.
- **Enums Persisted as String:** Always use `@Enumerated(EnumType.STRING)` on entity fields.

---

## 4. Existing API Endpoint Inventory

| HTTP Method | Endpoint | Controller | Function / Description |
|:---|:---|:---|:---|
| `GET` | `/api/health` | `HealthController` | System status check (`UP` / timestamp) |
| `POST`| `/api/v1/auth/login` | `AuthController` | Email & password login, returns JWT + User payload |
| `POST`| `/api/v1/auth/google`| `AuthController` | Google OAuth login/registration via ID token or profile |
| `POST`| `/api/v1/auth/register`| `AuthController`| Register new learner account with BCrypt password |
| `GET` | `/api/v1/auth/me` | `AuthController` | Current authenticated user profile (`Authorization: Bearer <token>`) |
| `GET` | `/api/posts/catalog` | `ReadingPostController` | Discovery catalog grouped by `topicGroup`, multi-level cards, topic counts |
| `GET` | `/api/posts` | `ReadingPostController` | Filter posts by `topic`, `region`, `topicGroup`, `level`, `query` |
| `GET` | `/api/posts/{id}` | `ReadingPostController` | Post details with all bilingual paragraphs & vocab items |
| `POST` | `/api/posts` | `ReadingPostController` | Create new post (accepts full nested paragraph & vocab list) |
| `PUT` | `/api/posts/{id}` | `ReadingPostController` | Full update of existing post & nested relationships |
| `DELETE` | `/api/posts/{id}` | `ReadingPostController` | Delete post and cascade-delete child entities |
| `POST` | `/api/posts/{id}/read` | `ReadingPostController` | Increment read counter (+1 atomic read) |
| `GET` | `/api/voice/models` | `VoiceController` | List available Azure & AI voice models for admin selector |
| `POST` | `/api/voice/generate-track`| `VoiceController` | Generate dual-speed audio tracks (Slow -15%, Fast +15%) |
| `GET` | `/api/voice/voices` | `VoiceController` | List 5 Gemini AI voice models (Puck, Charon, Kore, Fenrir, Aoede) |
| `POST` | `/api/voice/generate` | `VoiceController` | Legacy Gemini WAV generation |
| `POST` | `/api/tts` | `TtsController` | Stream MP3 audio for word/text (Azure primary -> Gemini fallback -> MD5 cached) |

---

## 5. Agent Playbook: Roadmap to Create a New API Feature

Follow this sequential 8-step recipe when tasked to create a new module or API endpoint.

```
Step 1: Entity & Enums  ──►  Step 2: DTOs  ──►  Step 3: Repository  ──►  Step 4: Service
                                                                              │
Step 8: Build & Verify  ◄──  Step 7: Tests ◄──  Step 6: Swagger/CORS ◄── Step 5: Controller
```

### Step 1: Create Entity in `entity/<Feature>.java`
```java
package com.viecultures.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "features")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Feature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CefrLevel level;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

### Step 2: Create DTOs in `dto/<Feature>Request.java` and `dto/<Feature>Response.java`
- Keep Request and Response separated.
- Use `@Data @Builder @NoArgsConstructor @AllArgsConstructor`.

### Step 3: Create Repository in `repository/<Feature>Repository.java`
```java
package com.viecultures.backend.repository;

import com.viecultures.backend.entity.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeatureRepository extends JpaRepository<Feature, String>, JpaSpecificationExecutor<Feature> {
    Optional<Feature> findByNameIgnoreCase(String name);
}
```

### Step 4: Implement Service in `service/<Feature>Service.java`
```java
package com.viecultures.backend.service;

import com.viecultures.backend.dto.FeatureRequest;
import com.viecultures.backend.dto.FeatureResponse;
import com.viecultures.backend.entity.Feature;
import com.viecultures.backend.repository.FeatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureService {

    private final FeatureRepository featureRepository;

    @Transactional(readOnly = true)
    public FeatureResponse getById(String id) {
        Feature entity = featureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feature not found with id: " + id));
        return mapToResponse(entity);
    }

    @Transactional
    public FeatureResponse create(FeatureRequest request) {
        Feature entity = Feature.builder()
                .name(request.getName())
                .level(request.getLevel())
                .build();
        Feature saved = featureRepository.save(entity);
        return mapToResponse(saved);
    }

    private FeatureResponse mapToResponse(Feature entity) {
        return FeatureResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .level(entity.getLevel())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
```

### Step 5: Implement Controller in `controller/<Feature>Controller.java`
```java
package com.viecultures.backend.controller;

import com.viecultures.backend.dto.FeatureRequest;
import com.viecultures.backend.dto.FeatureResponse;
import com.viecultures.backend.service.FeatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/features")
@RequiredArgsConstructor
@Tag(name = "X. Features", description = "Quản lý tính năng mới")
public class FeatureController {

    private final FeatureService featureService;

    @Operation(summary = "Xem chi tiết feature")
    @GetMapping("/{id}")
    public ResponseEntity<FeatureResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(featureService.getById(id));
    }

    @Operation(summary = "Tạo mới feature")
    @PostMapping
    public ResponseEntity<FeatureResponse> create(@RequestBody FeatureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(featureService.create(request));
    }
}
```

### Step 6: Verify CORS & Static Paths
- If the endpoint produces audio files, verify that they are written into `audio.output.dir` (`../frontend/public/audio`) or `tts.cache.dir` (`audio-cache`).
- Note that `CorsConfig.java` automatically handles static exposure of `/audio/**`.

### Step 7: Write Unit Tests in `src/test/java/...`
- Use Mockito (`@ExtendWith(MockitoExtension.class)`) for Controller & Service tests. See `TtsControllerTest.java` as a standard reference.

### Step 8: Build & Verify
Run compilation check:
```powershell
.\mvnw.cmd test-compile
```

---

## 6. Agent Playbook: Code Review & Quality Checklist

Before finalizing any PR or backend code change, the Agent **must verify** against these 10 criteria:

| # | Inspection Point | Failure Condition / Risk | Remedy |
|:--|:---|:---|:---|
| 1 | **Layer Boundary** | Controller calls Repository directly or bypasses Service. | Route call through Service method. |
| 2 | **Entity Leakage** | Controller returns JPA Entity directly instead of DTO. | Create or map to a DTO with only required fields. |
| 3 | **Circular toString** | Entity with `@OneToMany` / `@ManyToOne` lacks `@ToString.Exclude`. | Add `@ToString.Exclude` on the relationship field. |
| 4 | **Transaction Safety** | Data mutation method lacks `@Transactional`. | Annotate mutation method with `@Transactional`. |
| 5 | **JPA ID Strategy** | New Entity uses manual ID without generator or generator mismatch. | Use `GenerationType.UUID` or `IDENTITY` consistently. |
| 6 | **Enums in Entities** | Entity enum missing `@Enumerated(EnumType.STRING)`. | Add `@Enumerated(EnumType.STRING)`. Ordinal breaks on reorder. |
| 7 | **SQL Injection** | Dynamic queries built with string concatenation in JPA. | Use `Specification`, `CriteriaBuilder`, or named parameters `:param`. |
| 8 | **Swagger Tagging** | Controller missing `@Tag` or method missing `@Operation`. | Add Swagger annotations so UI docs are kept updated. |
| 9 | **Connection Holding**| Lengthy external AI API call made inside an active `@Transactional` block. | Do external HTTP calls *before* or *after* the DB transaction to avoid exhausting the HikariCP pool. |
| 10| **Static Compilation**| Code changes not verified against Maven compiler. | Always execute `.\mvnw.cmd test-compile` before completing. |

---

## 7. Environment Configurations & External Services

| Variable Key | Default Value | Purpose |
|:---|:---|:---|
| `PORT` | `8080` | Web server port |
| `DB_URL` | Supabase AWS pooler string | PostgreSQL connection URL |
| `DB_USERNAME` | `postgres...` | PostgreSQL username |
| `DB_PASSWORD` | Secrets | PostgreSQL password |
| `JPA_DDL_AUTO` | `update` | Hibernate schema update strategy |
| `JPA_SHOW_SQL` | `false` | Log raw SQL statements to console |
| `TTS_PROVIDER` | `azure` | Primary TTS service (`azure` or `gemini`) |
| `AZURE_SPEECH_KEY` | empty | Secret key for Azure Cognitive Speech REST API |
| `AZURE_SPEECH_REGION`| `southeastasia` | Azure Speech region |
| `GEMINI_API_KEY` | empty | Google AI Studio Gemini API Key |
| `GEMINI_MODEL` | `gemini-3.8-flash` | Gemini model for AI voice synthesis |
| `AUDIO_OUTPUT_DIR` | `../frontend/public/audio` | Destination directory for generated post audio |
| `TTS_CACHE_DIR` | `audio-cache` | Disk cache directory for MD5 synthesized audio clips |
| `TTS_FALLBACK_ENABLED`| `true` | Auto-failover from Azure to Gemini or fallback voice |
| `CORS_ALLOWED_ORIGINS`| `localhost:*, viecultures.vercel.app` | Allowed CORS origins for browser fetch |

---

## 8. CLI Command Cheat Sheet (Windows / PowerShell)

Run all commands from `backend/`:

```powershell
# 1. Compile and verify syntax without running tests
.\mvnw.cmd test-compile

# 2. Run all unit tests
.\mvnw.cmd test

# 3. Start local development server
.\mvnw.cmd spring-boot:run

# 4. Package production JAR
.\mvnw.cmd clean package -DskipTests

# 5. Access Swagger UI (when server is running)
# URL: http://localhost:8080/swagger-ui.html
# OpenAPI JSON: http://localhost:8080/v3/api-docs
```
