package com.viecultures.backend.controller;

import com.viecultures.backend.dto.DictionaryLookupDto;
import com.viecultures.backend.service.DictionaryLookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/vocabularies")
@RequiredArgsConstructor
@Tag(name = "4. Vocabulary & Dictionary", description = "Tra cứu từ vựng thông minh, tự động làm giàu dữ liệu và lưu trữ xuống CSDL")
public class VocabController {

    private final DictionaryLookupService dictionaryLookupService;

    @Operation(summary = "Tra cứu từ vựng tức thì (On-Demand Lookup & Auto-persist)",
               description = "Tìm kiếm từ trong DB. Nếu chưa có, hệ thống sẽ tự động gọi API từ điển làm giàu và lưu xuống CSDL cho các lần sau.")
    @GetMapping("/lookup")
    public ResponseEntity<DictionaryLookupDto> lookupWord(
            @Parameter(description = "Từ hoặc cụm từ cần tra cứu", example = "serendipity")
            @RequestParam String word,
            @Parameter(description = "Câu ngữ cảnh chứa từ này", required = false)
            @RequestParam(required = false) String contextSentence) {
        DictionaryLookupDto result = dictionaryLookupService.lookupWord(word, contextSentence);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Gợi ý từ vựng khi gõ (Autocomplete Search)",
               description = "Tìm kiếm nhanh các từ vựng đã có trong DB bắt đầu bằng prefix.")
    @GetMapping("/search")
    public ResponseEntity<List<DictionaryLookupDto>> searchPrefix(
            @Parameter(description = "Tiền tố tìm kiếm", example = "cul")
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "10") int limit) {
        List<DictionaryLookupDto> results = dictionaryLookupService.searchPrefix(query, limit);
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "Danh sách từ vựng phổ biến",
               description = "Lấy top các từ vựng được tra cứu nhiều nhất để làm bộ flashcards.")
    @GetMapping("/popular")
    public ResponseEntity<List<DictionaryLookupDto>> getPopularWords(
            @RequestParam(defaultValue = "20") int limit) {
        List<DictionaryLookupDto> results = dictionaryLookupService.getPopularWords(limit);
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "Lấy danh sách từ vựng (Tương thích Mobile & Web Deck)",
               description = "Endpoint tương thích hỗ trợ Mobile app và Web lấy danh sách từ vựng theo search / category.")
    @GetMapping
    public ResponseEntity<List<DictionaryLookupDto>> getVocabularies(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "20") int limit) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(dictionaryLookupService.searchPrefix(search, limit));
        }
        return ResponseEntity.ok(dictionaryLookupService.getPopularWords(limit));
    }
}
