# Rule: Zero Hardcoding & Strict Fail-Fast Environment Variables (Backend)

## 1. Core Principle
* **Absolute Ban on Hardcoded Values:** Database connection strings, credentials, API tokens, storage paths, server ports, and CORS origins must be loaded via environment variables (`.env`).
* **Fail-Fast Enforcement:** Missing environment variables must cause the application to crash immediately upon bootstrap. Never supply silent fallback passwords or default URLs that mask configuration issues.

## 2. Configuration Conventions in application.properties
* **Mandatory Syntax (Fail-Fast):**
  ```properties
  spring.datasource.url=${DB_URL}
  spring.datasource.username=${DB_USERNAME}
  spring.datasource.password=${DB_PASSWORD}
  gemini.api.key=${GEMINI_API_KEY}
  azure.speech.key=${AZURE_SPEECH_KEY}
  ```
* **Banned Syntax:**
  ```properties
  # VIOLATION: Never hide database credentials with colon fallbacks!
  spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/viecultures}
  spring.datasource.password=${DB_PASSWORD:secret}
  ```

## 3. Required Environment Keys Checklist
* `DB_URL`: PostgreSQL JDBC connection URL (Supabase transaction pooler or local DB)
* `DB_USERNAME`: Database user
* `DB_PASSWORD`: Database password
* `GEMINI_API_KEY`: Google Gemini API key for AI TTS and cultural content analysis
* `AZURE_SPEECH_KEY`: Microsoft Azure Speech Services API Key
* `AZURE_SPEECH_REGION`: Region (e.g. `southeastasia`)
* `PORT`: Server port (defaults to 8080 when running locally)
* `CORS_ALLOWED_ORIGINS`: Comma-separated list of allowed frontend origins
