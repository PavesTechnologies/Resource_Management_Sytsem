package com.global_exception_handler;

import org.springframework.http.HttpStatus;

public class SkillExceptionHandler extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public SkillExceptionHandler(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static SkillExceptionHandler notFound(String message) {
        return new SkillExceptionHandler(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static SkillExceptionHandler badRequest(String message) {
        return new SkillExceptionHandler(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    public static SkillExceptionHandler conflict(String message) {
        return new SkillExceptionHandler(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    public HttpStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
}
