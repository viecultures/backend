# Startup Diagnostics & Troubleshooting Guide (Backend)

## 1. Overview
When launching `viecultures-backend` (via `./mvnw spring-boot:run` or IDE runner), common startup issues should be diagnosed and resolved immediately using the procedures below.

---

## 2. Issue 1: Database Connection Failure (Supabase / PostgreSQL)

### Symptom
Application crashes on startup with `CannotGetJdbcConnectionException` or `PSQLException: Connection to ... refused`:
```
[DATABASE ERROR] Cannot connect to PostgreSQL database.
Target URL: jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres
```

### Root Cause
1. `.env` file is missing or `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` are incorrect.
2. Supabase password contains special characters (`#`, `@`, `%`) that require URL encoding if included in the URI string.
3. Supabase IPv4 vs IPv6 incompatibility: direct connection (`5432`) may fail on networks without IPv6 support.

### Resolution Protocol
1. Verify `.env` exists in `backend/` and contains valid Supabase pooler credentials:
   ```properties
   DB_URL=jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require
   DB_USERNAME=postgres.[project-ref]
   DB_PASSWORD=your_secure_password
   ```
2. For Supabase, always use the **Transaction Pooler (Port 6543)** or **Session Pooler (Port 5432)** with `sslmode=require`.

---

## 3. Issue 2: Azure Speech / Gemini TTS Synthesis Error

### Symptom
`TTSController` or `AzureSpeechService` returns HTTP 500 or throws `SpeechSynthesisException`:
```
[TTS ERROR] Azure Speech synthesis failed: 401 Unauthorized / Resource Not Found
```

### Root Cause
* `AZURE_SPEECH_KEY` is empty, expired, or `AZURE_SPEECH_REGION` does not match the Azure resource location (e.g. `southeastasia` vs `eastus`).

### Resolution Protocol
1. Verify `.env` has:
   ```properties
   AZURE_SPEECH_KEY=your_azure_key
   AZURE_SPEECH_REGION=southeastasia
   ```
2. If Azure is unavailable, verify that fallback is enabled:
   ```properties
   tts.provider=gemini
   tts.fallback.enabled=true
   GEMINI_API_KEY=your_gemini_key
   ```

---

## 4. Issue 3: Audio Cache Directory Permission / Missing Path

### Symptom
`IOException: Cannot create directory audio-cache` or `FileNotFoundException` when saving `.wav` files.

### Resolution Protocol
1. Check `application.properties`:
   ```properties
   audio.output.dir=../frontend/public/audio
   tts.cache.dir=audio-cache
   ```
2. Ensure directory exists or ensure service initializes directory with `Files.createDirectories(path)` before saving.
