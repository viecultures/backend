package com.viecultures.backend.modules.speech.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO phản hồi kết quả xử lý nhận dạng giọng nói hai giai đoạn (Two-Pass STT).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TwoPassSttResponse {

    /**
     * Văn bản thô thu được từ Pass 1 (Acoustic Transcription).
     */
    private String rawText;

    /**
     * Văn bản đã qua xử lý chuẩn hóa từ Pass 2 (Semantic Refinement).
     */
    private String cleanedText;

    /**
     * Danh sách các chỉnh sửa chi tiết được thực hiện ở Pass 2.
     */
    private List<SttCorrectionDetail> corrections;

    /**
     * Điểm số độ tin cậy của bản ghi (0.0 đến 1.0).
     */
    private double confidence;

    /**
     * Tổng thời gian xử lý toàn bộ quá trình (mili-giây).
     */
    private long totalLatencyMs;

    /**
     * Thời gian xử lý giai đoạn 1 - Nhận dạng âm học (mili-giây).
     */
    private long pass1LatencyMs;

    /**
     * Thời gian xử lý giai đoạn 2 - Tinh chỉnh ngữ nghĩa (mili-giây).
     */
    private long pass2LatencyMs;
}
