package com.viecultures.backend.service;

import com.viecultures.backend.exception.TtsException;
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

    @Value("${azure.speech.key}")
    private String azureSpeechKey;

    @Value("${azure.speech.region}")
    private String azureSpeechRegion;

    public AzureSpeechService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean isConfigured() {
        return azureSpeechKey != null &&
                !azureSpeechKey.trim().isEmpty() &&
                !azureSpeechKey.equalsIgnoreCase("your_key_here") &&
                !azureSpeechKey.equalsIgnoreCase("your_azure_speech_key_here");
    }

    /**
     * Gọi trực tiếp Azure Speech REST API để chuyển đổi văn bản sang file âm thanh
     * MP3
     */
    public byte[] synthesize(String text, String voice, String speed) {
        if (!isConfigured()) {
            throw new TtsException("AZURE_UNAUTHORIZED", 401,
                    "Chưa cấu hình AZURE_SPEECH_KEY hoặc key đang mang giá trị mẫu trong file .env. Vui lòng cập nhật API key.");
        }

        String targetVoice = (voice == null || voice.trim().isEmpty()) ? "en-US-JennyNeural" : mapVoice(voice.trim());
        String targetSpeed = (speed == null || speed.trim().isEmpty()) ? "0%" : formatSpeed(speed.trim());
        String ssml = buildSsml(text, targetVoice, targetSpeed);

        String endpoint = String.format("https://%s.tts.speech.microsoft.com/cognitiveservices/v1",
                (azureSpeechRegion != null && !azureSpeechRegion.trim().isEmpty()) ? azureSpeechRegion.trim()
                        : "southeastasia");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Ocp-Apim-Subscription-Key", azureSpeechKey.trim());
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
                        "Azure Speech API trả về phản hồi rỗng không chứa dữ liệu âm thanh");
            }
            log.info("Azure Speech API generated successfully: bytes={}", body.length);
            return body;

        } catch (HttpStatusCodeException ex) {
            int status = ex.getStatusCode().value();
            String responseBody = ex.getResponseBodyAsString();
            log.error("Azure Speech REST API returned HTTP {}: {}", status, responseBody);

            if (status == 401) {
                throw new TtsException("AZURE_UNAUTHORIZED", 401,
                        "Azure Speech API Key không hợp lệ hoặc đã hết hạn (HTTP 401). Vui lòng kiểm tra AZURE_SPEECH_KEY trong .env");
            } else if (status == 429) {
                throw new TtsException("AZURE_RATE_LIMITED", 429,
                        "Đã vượt quá hạn mức request hoặc số lượng ký tự hàng tháng của gói Free Azure F0 (HTTP 429).");
            } else if (status == 400) {
                throw new TtsException("AZURE_BAD_REQUEST", 400,
                        "Lỗi định dạng cấu trúc SSML hoặc tham số không hợp lệ từ Azure (HTTP 400): " + responseBody);
            } else {
                throw new TtsException("AZURE_API_ERROR", status,
                        "Lỗi giao tiếp với Azure Speech API (HTTP " + status + "): " + responseBody);
            }
        } catch (TtsException te) {
            throw te;
        } catch (Exception ex) {
            log.error("Unexpected exception invoking Azure Speech API: {}", ex.getMessage());
            throw new TtsException("AZURE_INTERNAL_ERROR", 500,
                    "Không thể kết nối tới máy chủ Azure Speech API: " + ex.getMessage());
        }
    }

    /**
     * Chuẩn hóa cấu trúc SSML (Speech Synthesis Markup Language) chuẩn XML
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
     * Tự động ánh xạ tên giọng Gemini nếu client truyền voice Gemini sang Azure
     * voice
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
