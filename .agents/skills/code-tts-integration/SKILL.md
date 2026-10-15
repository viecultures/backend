---
name: code-tts-integration
description: Manages Azure Speech and Gemini AI Voice audio synthesis, caching strategies, and voice track persistence.
---

# TTS Audio Integration & Cache Management

Handles multi-provider Text-to-Speech (Azure Speech SDK & Gemini Audio) with robust disk caching.

## Directives
1. **Cache-First Strategy:**
   * Before making any network call to Azure Speech or Gemini API, calculate an MD5/SHA256 hash of `(text + voiceModel + speed)`.
   * Check if `<cacheDir>/<hash>.wav` exists. If present, immediately return the cached URL/path.
2. **Provider Switching & Fallback:**
   * Primary Provider: Configured via `tts.provider=azure`.
   * Fallback Provider: If Azure returns 401/429/timeout and `tts.fallback.enabled=true`, seamlessly switch to Gemini Voice API.
3. **Database VoiceTrack Record:**
   * Persist generated audio metadata into `voice_tracks` table:
     - `reading_post_id`, `paragraph_index`, `audio_url`, `duration_ms`, `provider`, `voice_name`.
