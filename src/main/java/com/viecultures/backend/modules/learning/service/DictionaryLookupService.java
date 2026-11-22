package com.viecultures.backend.modules.learning.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viecultures.backend.modules.learning.dto.DictionaryLookupDto;
import com.viecultures.backend.entity.DictionaryEntry;
import com.viecultures.backend.repository.DictionaryEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DictionaryLookupService {

    private final DictionaryEntryRepository dictionaryEntryRepository;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(2500))
            .build();

    /**
     * Tra cứu từ vựng: Kiểm tra CSDL trước, nếu chưa có thì gọi API ngoài và lưu xuống CSDL.
     */
    @Transactional
    public DictionaryLookupDto lookupWord(String rawWord, String contextSentence) {
        String cleanWord = sanitizeWord(rawWord);
        if (cleanWord.isBlank()) {
            return DictionaryLookupDto.builder()
                    .word(rawWord)
                    .pos("word")
                    .ipa("/-/")
                    .viMeaning("Không tìm thấy từ vựng hợp lệ.")
                    .contextSentence(contextSentence)
                    .fromCache(false)
                    .build();
        }

        String searchKey = cleanWord.toLowerCase();

        // 1. Kiểm tra trong Cơ sở dữ liệu (Cache Hit)
        Optional<DictionaryEntry> existingOpt = dictionaryEntryRepository.findByWordIgnoreCase(searchKey);
        if (existingOpt.isPresent()) {
            DictionaryEntry entry = existingOpt.get();
            entry.setSearchCount(entry.getSearchCount() == null ? 1 : entry.getSearchCount() + 1);
            if ((entry.getContextSentence() == null || entry.getContextSentence().isBlank())
                    && contextSentence != null && !contextSentence.isBlank()) {
                entry.setContextSentence(contextSentence);
            }
            dictionaryEntryRepository.save(entry);
            log.info("[DictionaryLookup] Cache HIT từ DB cho từ: '{}' (searchCount={})", searchKey, entry.getSearchCount());
            return DictionaryLookupDto.fromEntity(entry, true, contextSentence);
        }

        // 2. Cache Miss: Gọi API bên ngoài không đồng bộ để làm giàu thông tin
        log.info("[DictionaryLookup] Cache MISS cho từ: '{}' -> Đang gọi API ngoài và lưu DB...", searchKey);
        DictionaryEntry newEntry = fetchAndEnrichWord(cleanWord, contextSentence);

        try {
            DictionaryEntry saved = dictionaryEntryRepository.save(newEntry);
            log.info("[DictionaryLookup] Đã lưu thành công từ mới vào DB: '{}' (ID: {})", saved.getWord(), saved.getId());
            return DictionaryLookupDto.fromEntity(saved, false, contextSentence);
        } catch (Exception e) {
            log.warn("[DictionaryLookup] Lỗi khi lưu từ vào DB (có thể trùng lặp concurrent): {}", e.getMessage());
            // Retry fetch in case of concurrent insert
            return dictionaryEntryRepository.findByWordIgnoreCase(searchKey)
                    .map(entry -> DictionaryLookupDto.fromEntity(entry, true, contextSentence))
                    .orElseGet(() -> DictionaryLookupDto.fromEntity(newEntry, false, contextSentence));
        }
    }

    /**
     * Gợi ý từ khi gõ tìm kiếm (Autocomplete)
     */
    @Transactional(readOnly = true)
    public List<DictionaryLookupDto> searchPrefix(String prefix, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return dictionaryEntryRepository.findByWordStartingWithIgnoreCaseOrderBySearchCountDesc(
                        prefix.trim().toLowerCase(),
                        PageRequest.of(0, safeLimit)
                ).stream()
                .map(e -> DictionaryLookupDto.fromEntity(e, true, ""))
                .toList();
    }

    /**
     * Lấy danh sách các từ vựng phổ biến nhất (phục vụ bộ ôn tập flashcard)
     */
    @Transactional(readOnly = true)
    public List<DictionaryLookupDto> getPopularWords(int limit) {
        return dictionaryEntryRepository.findTop30ByOrderBySearchCountDesc().stream()
                .limit(limit)
                .map(e -> DictionaryLookupDto.fromEntity(e, true, ""))
                .toList();
    }

    private DictionaryEntry fetchAndEnrichWord(String word, String contextSentence) {
        String lowerWord = word.toLowerCase();

        CompletableFuture<DatamuseResult> datamuseFuture = CompletableFuture.supplyAsync(() -> fetchDatamuse(lowerWord));
        CompletableFuture<String> viMeaningFuture = CompletableFuture.supplyAsync(() -> fetchVietnameseTranslation(word));
        CompletableFuture<FreeDictResult> freeDictFuture = CompletableFuture.supplyAsync(() -> fetchFreeDictionary(lowerWord));

        DatamuseResult datamuse = datamuseFuture.join();
        String viMeaning = viMeaningFuture.join();
        FreeDictResult freeDict = freeDictFuture.join();

        String ipa = (freeDict != null && freeDict.ipa != null) ? freeDict.ipa
                : (datamuse != null && datamuse.ipa != null ? datamuse.ipa : "/" + lowerWord + "/");

        String pos = (datamuse != null && datamuse.pos != null) ? datamuse.pos
                : (freeDict != null && freeDict.pos != null ? freeDict.pos : (word.contains(" ") ? "cụm từ" : "từ"));

        String enDef = (freeDict != null && freeDict.definition != null) ? freeDict.definition
                : (datamuse != null && datamuse.definition != null ? datamuse.definition : "Vocabulary item: " + word);

        String audioUrl = (freeDict != null && freeDict.audioUrl != null) ? freeDict.audioUrl : null;

        String safeViMeaning = (viMeaning != null && !viMeaning.isBlank()) ? viMeaning : "Từ tiếng Anh: " + word;

        return DictionaryEntry.builder()
                .word(lowerWord)
                .pos(pos)
                .ipa(ipa)
                .viMeaning(safeViMeaning)
                .enDefinition(enDef)
                .contextSentence(contextSentence != null && !contextSentence.isBlank() ? contextSentence : (freeDict != null ? freeDict.example : null))
                .audioUrl(audioUrl)
                .level("B1")
                .searchCount(1)
                .build();
    }

    private String fetchVietnameseTranslation(String word) {
        try {
            String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=vi&dt=t&q="
                    + URLEncoder.encode(word, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(2500))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.isArray() && root.size() > 0 && root.get(0).isArray() && root.get(0).size() > 0) {
                    JsonNode first = root.get(0).get(0);
                    if (first.isArray() && first.size() > 0) {
                        return first.get(0).asText();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[DictionaryLookup] Lỗi Google Translate cho '{}': {}", word, e.getMessage());
        }
        return null;
    }

    private DatamuseResult fetchDatamuse(String word) {
        try {
            String url = "https://api.datamuse.com/words?sp=" + URLEncoder.encode(word, StandardCharsets.UTF_8)
                    + "&md=dpr&ipa=1&max=1";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(2500))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode array = objectMapper.readTree(response.body());
                if (array.isArray() && array.size() > 0) {
                    JsonNode item = array.get(0);
                    DatamuseResult res = new DatamuseResult();
                    if (item.has("tags") && item.get("tags").isArray()) {
                        for (JsonNode tag : item.get("tags")) {
                            String t = tag.asText("");
                            if (t.startsWith("ipa_pron:")) {
                                String cleanIpa = t.replace("ipa_pron:", "").trim();
                                res.ipa = cleanIpa.startsWith("/") ? cleanIpa : "/" + cleanIpa + "/";
                            } else if (List.of("n", "v", "adj", "adv").contains(t)) {
                                res.pos = mapPosTag(t);
                            }
                        }
                    }
                    if (item.has("defs") && item.get("defs").isArray() && item.get("defs").size() > 0) {
                        String defLine = item.get("defs").get(0).asText("");
                        String[] parts = defLine.split("\t");
                        if (parts.length > 1) {
                            res.definition = parts[1].trim();
                            if (res.pos == null) res.pos = mapPosTag(parts[0]);
                        } else {
                            res.definition = defLine.trim();
                        }
                    }
                    return res;
                }
            }
        } catch (Exception e) {
            log.debug("[DictionaryLookup] Lỗi Datamuse cho '{}': {}", word, e.getMessage());
        }
        return null;
    }

    private FreeDictResult fetchFreeDictionary(String word) {
        try {
            String url = "https://api.dictionaryapi.dev/api/v2/entries/en/" + URLEncoder.encode(word, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(2500))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode array = objectMapper.readTree(response.body());
                if (array.isArray() && array.size() > 0) {
                    JsonNode entry = array.get(0);
                    FreeDictResult res = new FreeDictResult();
                    if (entry.has("phonetic") && !entry.get("phonetic").isNull()) {
                        res.ipa = entry.get("phonetic").asText();
                    }
                    if (entry.has("phonetics") && entry.get("phonetics").isArray()) {
                        for (JsonNode ph : entry.get("phonetics")) {
                            if (res.ipa == null && ph.has("text")) {
                                res.ipa = ph.get("text").asText();
                            }
                            if (ph.has("audio") && !ph.get("audio").asText().isBlank()) {
                                res.audioUrl = ph.get("audio").asText();
                                break;
                            }
                        }
                    }
                    if (entry.has("meanings") && entry.get("meanings").isArray() && entry.get("meanings").size() > 0) {
                        JsonNode meaning = entry.get("meanings").get(0);
                        if (meaning.has("partOfSpeech")) {
                            res.pos = meaning.get("partOfSpeech").asText();
                        }
                        if (meaning.has("definitions") && meaning.get("definitions").isArray() && meaning.get("definitions").size() > 0) {
                            JsonNode d = meaning.get("definitions").get(0);
                            if (d.has("definition")) res.definition = d.get("definition").asText();
                            if (d.has("example")) res.example = d.get("example").asText();
                        }
                    }
                    return res;
                }
            }
        } catch (Exception e) {
            log.debug("[DictionaryLookup] Lỗi FreeDictionary cho '{}': {}", word, e.getMessage());
        }
        return null;
    }

    private String mapPosTag(String tag) {
        return switch (tag.toLowerCase()) {
            case "n" -> "Danh từ (noun)";
            case "v" -> "Động từ (verb)";
            case "adj" -> "Tính từ (adjective)";
            case "adv" -> "Phó từ (adverb)";
            default -> tag;
        };
    }

    private String sanitizeWord(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$", "").trim();
    }

    private static class DatamuseResult {
        String ipa;
        String pos;
        String definition;
    }

    private static class FreeDictResult {
        String ipa;
        String pos;
        String definition;
        String example;
        String audioUrl;
    }
}
