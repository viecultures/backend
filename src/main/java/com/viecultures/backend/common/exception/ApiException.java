package com.viecultures.backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.status = errorCode.getStatus();
    }

    public ApiException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.status = errorCode.getStatus();
    }

    public ApiException(HttpStatus status, String message, String errorCodeStr) {
        super(message);
        this.status = status;
        this.errorCode = new ErrorCode() {
            @Override
            public HttpStatus getStatus() {
                return status;
            }

            @Override
            public String getMessage() {
                return message;
            }

            @Override
            public String getErrorCode() {
                return errorCodeStr;
            }
        };
    }
}
