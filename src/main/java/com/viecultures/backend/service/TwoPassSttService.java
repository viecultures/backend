package com.viecultures.backend.service;

import com.viecultures.backend.dto.TwoPassSttResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Giao diện dịch vụ Two-Pass Speech-to-Text.
 * Pass 1: Nhận dạng âm học nguyên văn (Acoustic Transcription) qua mô hình đa phương thức Gemini.
 * Pass 2: Tinh chỉnh và làm sạch ngữ nghĩa (Semantic Refinement & Cleanup) loại bỏ filler words, sửa câu tự chỉnh.
 */
public interface TwoPassSttService {

    /**
     * Nhận dạng và làm sạch giọng nói từ tệp âm thanh tải lên.
     *
     * @param file tệp âm thanh (WAV hoặc WebM)
     * @return TwoPassSttResponse chứa rawText, cleanedText, corrections và latency metrics
     */
    TwoPassSttResponse transcribe(MultipartFile file);
}
