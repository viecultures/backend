package com.viecultures.backend.common.exception;

import com.viecultures.backend.common.response.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex, HttpServletRequest request) {
        log.warn("API Exception [{}]: {} at {}", ex.getStatus(), ex.getMessage(), request.getRequestURI());
        String code = ex.getErrorCode() != null ? ex.getErrorCode().getErrorCode() : "API_ERROR";
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage(), code));
    }

    @ExceptionHandler(TtsException.class)
    public ResponseEntity<ApiResponse<Void>> handleTtsException(TtsException ex, HttpServletRequest request) {
        log.warn("TTS Exception [{}]: {} at {}", ex.getStatus(), ex.getMessage(), request.getRequestURI());
        HttpStatus status = HttpStatus.resolve(ex.getStatus());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status)
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        if (errorMessage.isBlank()) {
            errorMessage = "Dữ liệu yêu cầu không hợp lệ";
        }
        log.warn("Validation error at {}: {}", request.getRequestURI(), errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorMessage, "VALIDATION_FAILED"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Unauthorized access at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Thông tin đăng nhập không chính xác", "UNAUTHORIZED"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Bạn không có quyền thực hiện thao tác này", "FORBIDDEN"));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFoundException(EntityNotFoundException ex, HttpServletRequest request) {
        log.warn("Entity not found at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), "NOT_FOUND"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Bad argument at {}: {}", request != null ? request.getRequestURI() : "", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), "INVALID_ARGUMENT"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException ex, HttpServletRequest request) {
        log.warn("Illegal state at {}: {}", request != null ? request.getRequestURI() : "", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), "ACTION_NOT_ALLOWED"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        log.warn("Response status exception [{}] at {}: {}", ex.getStatusCode(), request != null ? request.getRequestURI() : "", ex.getReason());
        String code = ex.getStatusCode().toString();
        String reason = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode())
                .body(ApiResponse.error(reason, code));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("File upload exceeds max size at {}: {}", request != null ? request.getRequestURI() : "", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("Kích thước tệp vượt quá giới hạn tối đa cho phép (5MB)", "FILE_TOO_LARGE"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {}: {}", request != null ? request.getRequestURI() : "", ex.getMessage(), ex);
        String message = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "Đã xảy ra lỗi hệ thống nội bộ. Vui lòng thử lại sau.";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message, "INTERNAL_SERVER_ERROR"));
    }
}
