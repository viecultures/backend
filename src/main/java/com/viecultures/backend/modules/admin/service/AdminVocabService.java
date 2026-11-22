package com.viecultures.backend.modules.admin.service;

import com.viecultures.backend.modules.learning.dto.DictionaryLookupDto;
import com.viecultures.backend.modules.admin.dto.AdminVocabRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminVocabService {

    default Page<DictionaryLookupDto> getAdminVocabularies(String search, String level, Pageable pageable) {
        return getAdminVocabularies(search, level, null, pageable);
    }

    Page<DictionaryLookupDto> getAdminVocabularies(String search, String level, String postId, Pageable pageable);

    DictionaryLookupDto getVocabularyById(String id);

    DictionaryLookupDto createVocabulary(AdminVocabRequest request);

    DictionaryLookupDto updateVocabulary(String id, AdminVocabRequest request);

    boolean deleteVocabulary(String id);
}
