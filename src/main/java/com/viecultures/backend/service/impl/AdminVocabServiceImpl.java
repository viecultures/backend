package com.viecultures.backend.service.impl;

import com.viecultures.backend.dto.DictionaryLookupDto;
import com.viecultures.backend.dto.admin.AdminVocabRequest;
import com.viecultures.backend.entity.DictionaryEntry;
import com.viecultures.backend.repository.DictionaryEntryRepository;
import com.viecultures.backend.service.AdminVocabService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminVocabServiceImpl implements AdminVocabService {

    private final DictionaryEntryRepository dictionaryEntryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<DictionaryLookupDto> getAdminVocabularies(String search, String level, Pageable pageable) {
        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String cleanLevel = (level != null && !level.trim().isEmpty() && !"ALL".equalsIgnoreCase(level.trim()))
                ? level.trim().toUpperCase()
                : null;

        log.info("[AdminVocabService] Lay danh sach tu vung: search='{}', level='{}', page={}, size={}",
                cleanSearch, cleanLevel, pageable.getPageNumber(), pageable.getPageSize());

        Page<DictionaryEntry> pageResult = dictionaryEntryRepository.searchVocabularies(cleanSearch, cleanLevel, pageable);
        return pageResult.map(DictionaryLookupDto::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public DictionaryLookupDto getVocabularyById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID tu vung khong hop le");
        }
        DictionaryEntry entry = dictionaryEntryRepository.findById(id.trim())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay tu vung voi ID: " + id));
        return DictionaryLookupDto.fromEntity(entry);
    }

    @Override
    @Transactional
    public DictionaryLookupDto createVocabulary(AdminVocabRequest request) {
        if (request == null || request.getWord() == null || request.getWord().trim().isEmpty()) {
            throw new IllegalArgumentException("Tu vung khong duoc de trong");
        }

        String normalizedWord = request.getWord().trim().toLowerCase();
        Optional<DictionaryEntry> existingOpt = dictionaryEntryRepository.findByWordIgnoreCase(normalizedWord);

        DictionaryEntry targetEntry;
        if (existingOpt.isPresent()) {
            targetEntry = existingOpt.get();
            log.info("[AdminVocabService] Upsert: Cap nhat thong tin tu vung da ton tai '{}' (ID: {})",
                    normalizedWord, targetEntry.getId());

            if (request.getPos() != null) {
                targetEntry.setPos(request.getPos().trim());
            }
            if (request.getIpa() != null) {
                targetEntry.setIpa(request.getIpa().trim());
            }
            if (request.getViMeaning() != null) {
                targetEntry.setViMeaning(request.getViMeaning().trim());
            }
            if (request.getEnDefinition() != null) {
                targetEntry.setEnDefinition(request.getEnDefinition().trim());
            }
            if (request.getContextSentence() != null) {
                targetEntry.setContextSentence(request.getContextSentence().trim());
            }
            if (request.getLevel() != null && !request.getLevel().trim().isEmpty()) {
                targetEntry.setLevel(request.getLevel().trim().toUpperCase());
            }
            if (request.getAudioUrl() != null) {
                targetEntry.setAudioUrl(request.getAudioUrl().trim());
            }
        } else {
            String initialLevel = (request.getLevel() != null && !request.getLevel().trim().isEmpty())
                    ? request.getLevel().trim().toUpperCase()
                    : "B1";

            targetEntry = DictionaryEntry.builder()
                    .word(normalizedWord)
                    .pos(request.getPos() != null ? request.getPos().trim() : null)
                    .ipa(request.getIpa() != null ? request.getIpa().trim() : null)
                    .viMeaning(request.getViMeaning() != null ? request.getViMeaning().trim() : null)
                    .enDefinition(request.getEnDefinition() != null ? request.getEnDefinition().trim() : null)
                    .contextSentence(request.getContextSentence() != null ? request.getContextSentence().trim() : null)
                    .level(initialLevel)
                    .audioUrl(request.getAudioUrl() != null ? request.getAudioUrl().trim() : null)
                    .searchCount(1)
                    .build();

            log.info("[AdminVocabService] Tao moi tu vung '{}' voi level='{}'", normalizedWord, initialLevel);
        }

        DictionaryEntry saved = dictionaryEntryRepository.save(targetEntry);
        log.info("[AdminVocabService] Luu thanh cong tu vung '{}' voi ID: {}", saved.getWord(), saved.getId());
        return DictionaryLookupDto.fromEntity(saved);
    }

    @Override
    @Transactional
    public DictionaryLookupDto updateVocabulary(String id, AdminVocabRequest request) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID tu vung khong hop le");
        }
        if (request == null) {
            throw new IllegalArgumentException("Du lieu cap nhat khong hop le");
        }

        DictionaryEntry entry = dictionaryEntryRepository.findById(id.trim())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay tu vung voi ID: " + id));

        if (request.getWord() != null && !request.getWord().trim().isEmpty()) {
            String newWord = request.getWord().trim().toLowerCase();
            if (!newWord.equalsIgnoreCase(entry.getWord())) {
                Optional<DictionaryEntry> duplicate = dictionaryEntryRepository.findByWordIgnoreCase(newWord);
                if (duplicate.isPresent() && !duplicate.get().getId().equals(entry.getId())) {
                    throw new IllegalArgumentException("Tu vung '" + newWord + "' da ton tai trong he thong");
                }
                entry.setWord(newWord);
            }
        }

        if (request.getPos() != null) {
            entry.setPos(request.getPos().trim());
        }
        if (request.getIpa() != null) {
            entry.setIpa(request.getIpa().trim());
        }
        if (request.getViMeaning() != null) {
            entry.setViMeaning(request.getViMeaning().trim());
        }
        if (request.getEnDefinition() != null) {
            entry.setEnDefinition(request.getEnDefinition().trim());
        }
        if (request.getContextSentence() != null) {
            entry.setContextSentence(request.getContextSentence().trim());
        }
        if (request.getLevel() != null && !request.getLevel().trim().isEmpty()) {
            entry.setLevel(request.getLevel().trim().toUpperCase());
        }
        if (request.getAudioUrl() != null) {
            entry.setAudioUrl(request.getAudioUrl().trim());
        }

        DictionaryEntry saved = dictionaryEntryRepository.save(entry);
        log.info("[AdminVocabService] Cap nhat thanh cong tu vung ID='{}' (word='{}')", saved.getId(), saved.getWord());
        return DictionaryLookupDto.fromEntity(saved);
    }

    @Override
    @Transactional
    public boolean deleteVocabulary(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID tu vung khong hop le");
        }

        DictionaryEntry entry = dictionaryEntryRepository.findById(id.trim())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay tu vung voi ID: " + id));

        dictionaryEntryRepository.delete(entry);
        log.info("[AdminVocabService] Da xoa tu vung khoi he thong: word='{}', ID='{}'", entry.getWord(), id);
        return true;
    }
}
