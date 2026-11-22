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
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Phương thức HTTP không được hỗ trợ", "METHOD_NOT_ALLOWED"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "Vượt quá giới hạn yêu cầu (Rate limit exceeded)", "TOO_MANY_REQUESTS"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống nội bộ", "INTERNAL_SERVER_ERROR");

    private final HttpStatus status;
    private final String message;
    private final String errorCode;
}
