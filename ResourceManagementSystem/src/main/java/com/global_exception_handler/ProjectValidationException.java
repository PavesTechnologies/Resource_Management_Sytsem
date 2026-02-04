package com.global_exception_handler;

import org.springframework.http.HttpStatus;

public class ProjectValidationException extends RuntimeException{
    private final HttpStatus status;
    private final String errorCode;

    public ProjectValidationException(
            HttpStatus status,
            String errorCode,
            String message
    ) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
