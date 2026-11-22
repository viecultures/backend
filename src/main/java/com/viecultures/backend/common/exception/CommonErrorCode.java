package com.viecultures.backend.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ", "INVALID_REQUEST"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Chưa xác thực hoặc phiên đăng nhập hết hạn", "UNAUTHORIZED"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Không có quyền truy cập", "FORBIDDEN"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Tài nguyên không tồn tại", "NOT_FOUND"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng", "USER_NOT_FOUND"),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy bài viết", "POST_NOT_FOUND"),
    JOURNEY_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy hành trình", "JOURNEY_NOT_FOUND"),
    VOCAB_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy từ vựng", "VOCAB_NOT_FOUND"),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "Kích thước tệp vượt quá giới hạn cho phép (tối đa 5MB)", "FILE_TOO_LARGE"),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "Định dạng tệp không được hỗ trợ", "INVALID_FILE_TYPE"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Phương thức HTTP không được hỗ trợ", "METHOD_NOT_ALLOWED"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "Vượt quá giới hạn yêu cầu (Rate limit exceeded)", "TOO_MANY_REQUESTS"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống nội bộ", "INTERNAL_SERVER_ERROR");

    private final HttpStatus status;
    private final String message;
    private final String errorCode;
}
