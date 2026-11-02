package com.viecultures.backend.service;

import com.viecultures.backend.dto.DictionaryLookupDto;
import com.viecultures.backend.dto.admin.AdminVocabRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminVocabService {

    Page<DictionaryLookupDto> getAdminVocabularies(String search, String level, Pageable pageable);

    DictionaryLookupDto getVocabularyById(String id);

    DictionaryLookupDto createVocabulary(AdminVocabRequest request);

    DictionaryLookupDto updateVocabulary(String id, AdminVocabRequest request);

    boolean deleteVocabulary(String id);
}
