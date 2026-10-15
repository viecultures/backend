---
name: code-sync-openapi-docs
description: Standardizes Springdoc OpenAPI annotations and verifies the API contract across backend endpoints.
---

# Sync OpenAPI Documentation

Ensures all REST API endpoints expose accurate OpenAPI/Swagger 3 schemas for Frontend integration.

## Directives
1. **Annotation Requirements:**
   Every controller method must define:
   ```java
   @Operation(
       summary = "Retrieve reading post by slug",
       description = "Returns bilingual paragraphs, cultural vocabulary, and audio track metadata"
   )
   @ApiResponses(value = {
       @ApiResponse(responseCode = "200", description = "Post retrieved successfully"),
       @ApiResponse(responseCode = "404", description = "Reading post not found", 
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
   })
   @GetMapping("/{slug}")
   public ResponseEntity<ApiResponse<ReadingPostDto>> getPostBySlug(@PathVariable String slug) {
       ...
   }
   ```
2. **Schema Verification:**
   * Run the backend and navigate to `http://localhost:8080/v3/api-docs` or `http://localhost:8080/swagger-ui.html`.
   * Verify that request schemas (e.g., TSV import payload, voice selection) and response types match `document/04-agent-and-dev-guidelines/system-data-dictionary.md`.
