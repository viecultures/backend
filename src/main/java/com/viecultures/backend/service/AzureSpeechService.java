package com.viecultures.backend.service;

import com.viecultures.backend.common.exception.TtsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class AzureSpeechService {

    private final RestTemplate restTemplate;
    private final DynamicApiKeyService dynamicApiKeyService;

    @Value("${azure.speech.key:}")
    private String azureSpeechKey;

    @Value("${azure.speech.region:southeastasia}")
    private String azureSpeechRegion;

    public AzureSpeechService(RestTemplate restTemplate, DynamicApiKeyService dynamicApiKeyService) {
        this.restTemplate = restTemplate;
        this.dynamicApiKeyService = dynamicApiKeyService;
    }

    public boolean isConfigured() {
        String activeKey = dynamicApiKeyService.getActiveKey("AZURE", "TTS");
        if (activeKey != null && !activeKey.trim().isEmpty()
                && !activeKey.equalsIgnoreCase("your_key_here")
                && !activeKey.equalsIgnoreCase("your_azure_speech_key_here")) {
            return true;
        }

        return azureSpeechKey != null &&
                !azureSpeechKey.trim().isEmpty() &&
                !azureSpeechKey.equalsIgnoreCase("your_key_here") &&
                !azureSpeechKey.equalsIgnoreCase("your_azure_speech_key_here");
    }

    /**
     * Goi truc tiep Azure Speech REST API de chuyen doi van ban sang file am thanh MP3
     */
    public byte[] synthesize(String text, String voice, String speed) {
        String keyToUse = dynamicApiKeyService.getActiveKey("AZURE", "TTS");
        if (keyToUse == null || keyToUse.trim().isEmpty()) {
            keyToUse = azureSpeechKey;
        }

        if (keyToUse == null || keyToUse.trim().isEmpty()
                || keyToUse.equalsIgnoreCase("your_key_here")
                || keyToUse.equalsIgnoreCase("your_azure_speech_key_here")) {
            throw new TtsException("AZURE_UNAUTHORIZED", 401,
                    "Chua cau hinh AZURE_SPEECH_KEY hoac key dang mang gia tri mau trong file .env. Vui long cap nhat API key.");
        }

        String regionToUse = dynamicApiKeyService.getActiveAzureRegion();
        if (regionToUse == null || regionToUse.trim().isEmpty()) {
            regionToUse = (azureSpeechRegion != null && !azureSpeechRegion.trim().isEmpty())
                    ? azureSpeechRegion.trim() : "southeastasia";
        }

        String targetVoice = (voice == null || voice.trim().isEmpty()) ? "en-US-JennyNeural" : mapVoice(voice.trim());
        String targetSpeed = (speed == null || speed.trim().isEmpty()) ? "0%" : formatSpeed(speed.trim());
        String ssml = buildSsml(text, targetVoice, targetSpeed);

        String endpoint = String.format("https://%s.tts.speech.microsoft.com/cognitiveservices/v1", regionToUse.trim());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Ocp-Apim-Subscription-Key", keyToUse.trim());
        headers.setContentType(MediaType.parseMediaType("application/ssml+xml"));
        headers.set("X-Microsoft-OutputFormat", "audio-16khz-128kbitrate-mono-mp3");
        headers.set("User-Agent", "EnglishLearningApp");

        HttpEntity<String> entity = new HttpEntity<>(ssml, headers);

        try {
            log.info("Invoking Azure Speech REST API: endpoint={}, voice={}, speed={}, textLength={}",
                    endpoint, targetVoice, targetSpeed, text.length());

            ResponseEntity<byte[]> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, byte[].class);
            byte[] body = response.getBody();
            if (body == null || body.length == 0) {
                throw new TtsException("AZURE_EMPTY_RESPONSE", 500,
                        "Azure Speech API tra ve phan hoi rong khong chua du lieu am thanh");
            }
            log.info("Azure Speech API generated successfully: bytes={}", body.length);
            return body;

        } catch (HttpStatusCodeException ex) {
            int status = ex.getStatusCode().value();
            String responseBody = ex.getResponseBodyAsString();
            log.error("Azure Speech REST API returned HTTP {}: {}", status, responseBody);

            if (status == 401) {
                throw new TtsException("AZURE_UNAUTHORIZED", 401,
                        "Azure Speech API Key khong hop le hoac da het han (HTTP 401). Vui long kiem tra AZURE_SPEECH_KEY");
            } else if (status == 429) {
                throw new TtsException("AZURE_RATE_LIMITED", 429,
                        "Da vuot qua han muc request hoac so luong ky tu hang thang cua goi Free Azure F0 (HTTP 429).");
            } else if (status == 400) {
                throw new TtsException("AZURE_BAD_REQUEST", 400,
                        "Loi dinh dang cau truc SSML hoac tham so khong hop le tu Azure (HTTP 400): " + responseBody);
            } else {
                throw new TtsException("AZURE_API_ERROR", status,
                        "Loi giao tiep voi Azure Speech API (HTTP " + status + "): " + responseBody);
            }
        } catch (TtsException te) {
            throw te;
        } catch (Exception ex) {
            log.error("Unexpected exception invoking Azure Speech API: {}", ex.getMessage());
            throw new TtsException("AZURE_INTERNAL_ERROR", 500,
                    "Khong the ket noi toi may chu Azure Speech API: " + ex.getMessage());
        }
    }

    /**
     * Chuan hoa cau truc SSML (Speech Synthesis Markup Language) chuan XML
     */
    public String buildSsml(String text, String voice, String speed) {
        String escapedText = escapeXml(text);
        return String.format(
                "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'>" +
                        "<voice name='%s'>" +
                        "<prosody rate='%s'>%s</prosody>" +
                        "</voice>" +
                        "</speak>",
                voice, speed, escapedText);
    }

    private String escapeXml(String input) {
        if (input == null)
            return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String formatSpeed(String speed) {
        speed = speed.trim();
        if (speed.endsWith("%") || speed.matches("(?i)slow|fast|medium|default|x-slow|x-fast")) {
            return speed;
        }
        return speed + "%";
    }

    /**
     * Tu dong anh xa ten giong Gemini neu client truyen voice Gemini sang Azure voice
     */
    private String mapVoice(String voice) {
        if (voice.equalsIgnoreCase("Amanda") || voice.equalsIgnoreCase("en-US-AmandaMultilingualNeural")) {
            return "en-US-AmandaMultilingualNeural";
        }
        if (voice.equalsIgnoreCase("Dustin") || voice.equalsIgnoreCase("en-US-DustinMultilingualNeural")) {
            return "en-US-DustinMultilingualNeural";
        }
        if (voice.equalsIgnoreCase("Puck") || voice.equalsIgnoreCase("Fenrir") || voice.equalsIgnoreCase("Charon")
                || voice.equalsIgnoreCase("Guy")) {
            return "en-US-GuyNeural";
        }
        if (voice.equalsIgnoreCase("Aoede") || voice.equalsIgnoreCase("Kore") || voice.equalsIgnoreCase("Jenny")) {
            return "en-US-JennyNeural";
        }
        return voice;
    }
}
