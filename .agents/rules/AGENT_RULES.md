# VieCultures Backend Rules & Architectural Constraints

This document establishes mandatory coding guidelines, architectural patterns, and execution workflows for the `backend/` service (Spring Boot 3.3.4, Java 17, Spring Data JPA, PostgreSQL/Supabase, Springdoc OpenAPI).

---

## 1. Prime Directives (Non-Negotiable)

### 1.1. Documentation is the Single Source of Truth (SSOT)
* Before creating or modifying any Controller endpoint, JPA Entity, or Service logic, verify the requirement against the `document/` repository (or root `document/` in the workspace).
* Never invent database fields, status enums, or business calculations without documenting them first.
* If an incoming request is ambiguous or alters existing business flows (e.g. CEFR levels, audio generation parameters, reflection moderation), STOP, propose options, confirm with the user, and update docs first.

### 1.2. Zero Hardcoding & Strict Fail-Fast Configuration
* All runtime variables (database credentials, API keys, storage directories, CORS origins) MUST reside in `.env` and be mapped into `application.properties`.
* **Fail-Fast Rule:** Never use colon default fallback syntax (e.g. `${DB_PASSWORD:secret}`). If a required variable is missing, the application MUST crash immediately at startup so misconfigurations are caught instantly.
* See [strict-env-configuration.md](strict-env-configuration.md) for details.

### 1.3. Standardized API Response Envelope
* Every `@RestController` endpoint must return a unified `ApiResponse<T>` envelope:
  ```json
  {
    "success": true,
    "message": "Operation successful",
    "data": { ... },
    "timestamp": "2026-10-15T10:00:00"
  }
  ```
* Error handling must be centralized in `@RestControllerAdvice` (`GlobalExceptionHandler`), returning structured `ApiResponse` with `success: false` and appropriate HTTP status codes.

---

## 2. Architecture & Code Conventions

### 2.1. Package Structure
```
com.viecultures.backend/
├── config/              # Security, OpenAPI (Swagger), WebMvc, CORS, TTS configs
├── controller/          # REST Controllers (@Tag, @Operation, @ApiResponse)
├── dto/                 # Request/Response DTOs (Jakarta Validation)
├── entity/              # JPA Entities (@Entity, @Table, audit fields)
├── exception/           # BusinessException, ResourceNotFoundException, GlobalExceptionHandler
├── repository/          # Spring Data JPA Repositories
└── service/             # Business logic interfaces & implementation services
```

### 2.2. Entity & JPA Guidelines
1. **Primary Keys:** Use `Long` ID with `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
2. **Audit Timestamps:** Include `createdAt` and `updatedAt` (`@CreationTimestamp`, `@UpdateTimestamp`).
3. **Indexing:** Add `@Index` on high-frequency query columns (e.g. `slug`, `province`, `cefrLevel`, `status`).
4. **Validation:** Annotate DTO fields with Jakarta Validation (`@NotBlank`, `@NotNull`, `@Size`, `@Min`).

### 2.3. TTS & External Service Reliability
1. **Azure Speech & Gemini TTS:**
   * TTS audio synthesis must be cached locally or on cloud storage.
   * Check cache before dispatching external TTS synthesis requests.
   * Implement graceful fallback (Azure -> Gemini -> Web Speech API notice) if primary provider fails.
2. **Audio File Paths:** Externalize `audio.output.dir` and `tts.cache.dir` via environment variables.

### 2.4. OpenAPI / Swagger Annotations
* Every controller must have `@Tag(name = "...", description = "...")`.
* Every endpoint method must have `@Operation(summary = "...", description = "...")` and `@ApiResponses`.
* Endpoint paths must follow REST conventions: `/api/v1/...` (e.g. `/api/v1/posts`, `/api/v1/tts`).

---

## 3. Step-by-Step Backend Development Workflow

1. **Verify Documentation:** Check `document/03-product-spec/` and `document/04-agent-and-dev-guidelines/system-data-dictionary.md`.
2. **Entity & Repository:** Create JPA entity and repository interface with optimized query methods.
3. **DTOs:** Create Request and Response DTOs with Jakarta Validation.
4. **Service:** Implement business logic, error throwing, and transactional boundaries (`@Transactional`).
5. **Controller:** Expose REST endpoint returning `ApiResponse<T>` with complete OpenAPI annotations.
6. **Verify:** Test endpoint via Swagger UI (`http://localhost:8080/swagger-ui.html`) or curl.
