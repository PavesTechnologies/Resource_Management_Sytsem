package com.global_exception_handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.Map;

public class GlobalPmsExceptionHandler {
    @ExceptionHandler(ProjectValidationException.class)
    public ResponseEntity<?> handleProjectException(ProjectValidationException ex) {

        return ResponseEntity
                .status(ex.getStatus())
                .body(Map.of(
                        "errorCode", ex.getErrorCode(),
                        "message", ex.getMessage(),
                        "timestamp", LocalDateTime.now()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnknown(Exception ex) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "errorCode", "INTERNAL_ERROR",
                        "message", "Unexpected system error"
                ));
    }
}
