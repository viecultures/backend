package com.viecultures.backend.common.exception;

import lombok.Getter;

@Getter
public class TtsException extends RuntimeException {
    private final String errorCode;
    private final int status;

    public TtsException(String errorCode, int status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}
