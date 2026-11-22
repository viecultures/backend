package com.viecultures.backend.modules.speech.service;

import com.viecultures.backend.modules.speech.dto.TwoPassSttResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Giao diện dịch vụ Two-Pass Speech-to-Text.
 * Pass 1: Nhận dạng âm học nguyên văn (Acoustic Transcription) qua mô hình đa phương thức Gemini.
 * Pass 2: Tinh chỉnh và làm sạch ngữ nghĩa (Semantic Refinement & Cleanup) loại bỏ filler words, sửa câu tự chỉnh.
 */
public interface TwoPassSttService {

    /**
     * Nhận dạng và làm sạch giọng nói từ tệp âm thanh tải lên với chế độ mặc định (smartCleanup = true).
     *
     * @param file tệp âm thanh (WAV hoặc WebM)
     * @return TwoPassSttResponse chứa rawText, cleanedText, corrections và latency metrics
     */
    default TwoPassSttResponse transcribe(MultipartFile file) {
        return transcribe(file, true);
    }

    /**
     * Nhận dạng giọng nói với tùy chọn làm sạch thông minh (smartCleanup).
     *
     * @param file tệp âm thanh (WAV hoặc WebM)
     * @param smartCleanup true: hội thoại thông minh (2-Pass), false: luyện phát âm (verbatim 100%)
     * @return TwoPassSttResponse chứa rawText, cleanedText, corrections và latency metrics
     */
    TwoPassSttResponse transcribe(MultipartFile file, boolean smartCleanup);
}
