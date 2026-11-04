package com.viecultures.backend.service.impl;

import com.viecultures.backend.dto.DictionaryLookupDto;
import com.viecultures.backend.dto.admin.AdminVocabRequest;
import com.viecultures.backend.entity.CefrLevel;
import com.viecultures.backend.entity.DictionaryEntry;
import com.viecultures.backend.entity.ReadingPost;
import com.viecultures.backend.entity.VocabItem;
import com.viecultures.backend.repository.DictionaryEntryRepository;
import com.viecultures.backend.repository.ReadingPostRepository;
import com.viecultures.backend.repository.VocabItemRepository;
import com.viecultures.backend.service.AdminVocabService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminVocabServiceImpl implements AdminVocabService {

    private final DictionaryEntryRepository dictionaryEntryRepository;
    private final VocabItemRepository vocabItemRepository;
    private final ReadingPostRepository readingPostRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<DictionaryLookupDto> getAdminVocabularies(String search, String level, String postId, Pageable pageable) {
        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String cleanLevel = (level != null && !level.trim().isEmpty() && !"ALL".equalsIgnoreCase(level.trim()))
                ? level.trim().toUpperCase()
                : null;
        String cleanPostId = (postId != null && !postId.trim().isEmpty()) ? postId.trim() : null;

        log.info("[AdminVocabService] Lay danh sach tu vung: search='{}', level='{}', postId='{}', page={}, size={}",
                cleanSearch, cleanLevel, cleanPostId, pageable.getPageNumber(), pageable.getPageSize());

        if (cleanPostId != null) {
            Specification<VocabItem> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("readingPost").get("id"), cleanPostId));

                if (cleanSearch != null) {
                    String pattern = "%" + cleanSearch.toLowerCase() + "%";
                    Predicate matchWord = cb.like(cb.lower(root.get("word")), pattern);
                    Predicate matchMeaning = cb.like(cb.lower(root.get("viMeaning")), pattern);
                    predicates.add(cb.or(matchWord, matchMeaning));
                }

                if (cleanLevel != null) {
                    try {
                        CefrLevel lvl = CefrLevel.valueOf(cleanLevel);
                        predicates.add(cb.equal(root.get("level"), lvl));
                    } catch (IllegalArgumentException ignored) {
                        log.debug("[AdminVocabService] Gia tri level khong hop le theo CefrLevel: {}", cleanLevel);
                    }
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            };

            Page<VocabItem> vocabPage = vocabItemRepository.findAll(spec, pageable);
            return vocabPage.map(v -> {
                DictionaryLookupDto dto = DictionaryLookupDto.fromVocabItem(v);
                dictionaryEntryRepository.findByWordIgnoreCase(v.getWord()).ifPresent(dict -> {
                    dto.setEnDefinition(dict.getEnDefinition());
                    dto.setSearchCount(dict.getSearchCount());
                });
                return dto;
            });
        }

        Page<DictionaryEntry> pageResult = dictionaryEntryRepository.searchVocabularies(cleanSearch, cleanLevel, pageable);
        return pageResult.map(entry -> {
            DictionaryLookupDto dto = DictionaryLookupDto.fromEntity(entry);
            enrichDtoWithPostInfo(dto);
            return dto;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public DictionaryLookupDto getVocabularyById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID tu vung khong hop le");
        }
        String cleanId = id.trim();

        Optional<DictionaryEntry> dictOpt = dictionaryEntryRepository.findById(cleanId);
        if (dictOpt.isPresent()) {
            DictionaryLookupDto dto = DictionaryLookupDto.fromEntity(dictOpt.get());
            enrichDtoWithPostInfo(dto);
            return dto;
        }

        Optional<VocabItem> vocabOpt = vocabItemRepository.findById(cleanId);
        if (vocabOpt.isPresent()) {
            VocabItem item = vocabOpt.get();
            DictionaryLookupDto dto = DictionaryLookupDto.fromVocabItem(item);
            dictionaryEntryRepository.findByWordIgnoreCase(item.getWord()).ifPresent(dict -> {
                dto.setEnDefinition(dict.getEnDefinition());
                dto.setSearchCount(dict.getSearchCount());
            });
            return dto;
        }

        throw new IllegalArgumentException("Khong tim thay tu vung voi ID: " + id);
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

        DictionaryEntry savedEntry = dictionaryEntryRepository.save(targetEntry);

        if (request.getPostId() != null && !request.getPostId().trim().isEmpty()) {
            linkVocabItemToPost(savedEntry, request.getPostId().trim(), request);
        }

        DictionaryLookupDto dto = DictionaryLookupDto.fromEntity(savedEntry);
        enrichDtoWithPostInfo(dto);
        return dto;
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
        String cleanId = id.trim();

        Optional<DictionaryEntry> dictOpt = dictionaryEntryRepository.findById(cleanId);
        if (dictOpt.isPresent()) {
            DictionaryEntry entry = dictOpt.get();

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

            if (request.getPostId() != null && !request.getPostId().trim().isEmpty()) {
                linkVocabItemToPost(saved, request.getPostId().trim(), request);
            } else {
                syncToExistingVocabItems(saved, request.getUsageNote());
            }

            DictionaryLookupDto dto = DictionaryLookupDto.fromEntity(saved);
            enrichDtoWithPostInfo(dto);
            return dto;
        }

        Optional<VocabItem> vocabOpt = vocabItemRepository.findById(cleanId);
        if (vocabOpt.isPresent()) {
            VocabItem vocab = vocabOpt.get();

            if (request.getWord() != null && !request.getWord().trim().isEmpty()) {
                vocab.setWord(request.getWord().trim().toLowerCase());
            }
            if (request.getPos() != null) {
                vocab.setPos(request.getPos().trim());
            }
            if (request.getIpa() != null) {
                vocab.setIpa(request.getIpa().trim());
            }
            if (request.getViMeaning() != null) {
                vocab.setViMeaning(request.getViMeaning().trim());
            }
            if (request.getContextSentence() != null) {
                vocab.setContextSentence(request.getContextSentence().trim());
            }
            if (request.getLevel() != null && !request.getLevel().trim().isEmpty()) {
                try {
                    vocab.setLevel(CefrLevel.valueOf(request.getLevel().trim().toUpperCase()));
                } catch (IllegalArgumentException ignored) {}
            }
            if (request.getAudioUrl() != null) {
                vocab.setAudioUrl(request.getAudioUrl().trim());
            }
            if (request.getUsageNote() != null) {
                vocab.setUsageNote(request.getUsageNote().trim());
            }

            if (request.getPostId() != null && !request.getPostId().trim().isEmpty()) {
                if (vocab.getReadingPost() == null || !request.getPostId().trim().equals(vocab.getReadingPost().getId())) {
                    ReadingPost newPost = readingPostRepository.findById(request.getPostId().trim())
                            .orElseThrow(() -> new IllegalArgumentException("Khong tim thay bai doc voi ID: " + request.getPostId()));
                    vocab.setReadingPost(newPost);
                }
            }

            VocabItem savedVocab = vocabItemRepository.save(vocab);

            DictionaryEntry dictEntry = dictionaryEntryRepository.findByWordIgnoreCase(savedVocab.getWord())
                    .orElseGet(() -> DictionaryEntry.builder()
                            .word(savedVocab.getWord())
                            .build());

            dictEntry.setPos(savedVocab.getPos());
            dictEntry.setIpa(savedVocab.getIpa());
            dictEntry.setViMeaning(savedVocab.getViMeaning());
            if (request.getEnDefinition() != null) {
                dictEntry.setEnDefinition(request.getEnDefinition().trim());
            }
            dictEntry.setContextSentence(savedVocab.getContextSentence());
            dictEntry.setLevel(savedVocab.getLevel() != null ? savedVocab.getLevel().name() : "B1");
            dictEntry.setAudioUrl(savedVocab.getAudioUrl());
            dictionaryEntryRepository.save(dictEntry);

            return DictionaryLookupDto.fromVocabItem(savedVocab);
        }

        throw new IllegalArgumentException("Khong tim thay tu vung voi ID: " + id);
    }

    @Override
    @Transactional
    public boolean deleteVocabulary(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID tu vung khong hop le");
        }
        String cleanId = id.trim();

        boolean deleted = false;
        Optional<DictionaryEntry> dictOpt = dictionaryEntryRepository.findById(cleanId);
        if (dictOpt.isPresent()) {
            dictionaryEntryRepository.delete(dictOpt.get());
            log.info("[AdminVocabService] Da xoa tu vung khoi tu dien: word='{}', ID='{}'", dictOpt.get().getWord(), cleanId);
            deleted = true;
        }

        Optional<VocabItem> vocabOpt = vocabItemRepository.findById(cleanId);
        if (vocabOpt.isPresent()) {
            vocabItemRepository.delete(vocabOpt.get());
            log.info("[AdminVocabService] Da xoa VocabItem lien ket: word='{}', ID='{}'", vocabOpt.get().getWord(), cleanId);
            deleted = true;
        }

        if (!deleted) {
            throw new IllegalArgumentException("Khong tim thay tu vung voi ID: " + id);
        }

        return true;
    }

    private void linkVocabItemToPost(DictionaryEntry entry, String postId, AdminVocabRequest request) {
        ReadingPost post = readingPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay bai doc voi ID: " + postId));

        String normalizedWord = entry.getWord().trim().toLowerCase();
        CefrLevel cefrLevel = parseCefrLevel(request.getLevel(), post.getLevel());

        Optional<VocabItem> existingVocabOpt = vocabItemRepository.findByReadingPostIdAndWordIgnoreCase(post.getId(), normalizedWord);
        if (existingVocabOpt.isPresent()) {
            VocabItem item = existingVocabOpt.get();
            item.setPos(entry.getPos());
            item.setIpa(entry.getIpa());
            item.setViMeaning(entry.getViMeaning());
            item.setContextSentence(entry.getContextSentence());
            item.setAudioUrl(entry.getAudioUrl());
            item.setLevel(cefrLevel);
            if (request.getUsageNote() != null) {
                item.setUsageNote(request.getUsageNote().trim());
            }
            vocabItemRepository.save(item);
            log.info("[AdminVocabService] Da cap nhat VocabItem '{}' lien ket voi bai doc '{}'", normalizedWord, post.getId());
        } else {
            VocabItem newVocab = VocabItem.builder()
                    .readingPost(post)
                    .word(normalizedWord)
                    .pos(entry.getPos())
                    .ipa(entry.getIpa())
                    .viMeaning(entry.getViMeaning())
                    .contextSentence(entry.getContextSentence())
                    .audioUrl(entry.getAudioUrl())
                    .level(cefrLevel)
                    .usageNote(request.getUsageNote() != null ? request.getUsageNote().trim() : null)
                    .build();
            vocabItemRepository.save(newVocab);
            log.info("[AdminVocabService] Da tao moi VocabItem '{}' lien ket voi bai doc '{}'", normalizedWord, post.getId());
        }
    }

    private void syncToExistingVocabItems(DictionaryEntry entry, String usageNote) {
        List<VocabItem> vocabItems = vocabItemRepository.findByWordIgnoreCase(entry.getWord());
        for (VocabItem item : vocabItems) {
            item.setPos(entry.getPos());
            item.setIpa(entry.getIpa());
            item.setViMeaning(entry.getViMeaning());
            if (entry.getContextSentence() != null && !entry.getContextSentence().isBlank()) {
                item.setContextSentence(entry.getContextSentence());
            }
            item.setAudioUrl(entry.getAudioUrl());
            if (usageNote != null && !usageNote.isBlank()) {
                item.setUsageNote(usageNote.trim());
            }
            vocabItemRepository.save(item);
        }
    }

    private void enrichDtoWithPostInfo(DictionaryLookupDto dto) {
        if (dto == null || dto.getWord() == null) {
            return;
        }
        vocabItemRepository.findFirstByWordIgnoreCase(dto.getWord().trim().toLowerCase()).ifPresent(vocab -> {
            if (dto.getUsageNote() == null || dto.getUsageNote().isBlank()) {
                dto.setUsageNote(vocab.getUsageNote());
            }
            ReadingPost post = vocab.getReadingPost();
            if (post != null) {
                dto.setPostId(post.getId());
                dto.setPostTitleEn(post.getTitleEn());
                dto.setPostTitleVi(post.getTitleVi());
                if (post.getJourney() != null) {
                    dto.setJourneyId(post.getJourney().getId());
                    dto.setJourneyTitleEn(post.getJourney().getTitleEn());
                }
            }
        });
    }

    private CefrLevel parseCefrLevel(String levelStr, CefrLevel fallback) {
        if (levelStr != null && !levelStr.trim().isEmpty()) {
            try {
                return CefrLevel.valueOf(levelStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        return fallback != null ? fallback : CefrLevel.B1;
    }
}
