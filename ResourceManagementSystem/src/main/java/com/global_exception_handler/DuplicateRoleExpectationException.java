package com.global_exception_handler;

public class DuplicateRoleExpectationException extends RuntimeException {
    
    public DuplicateRoleExpectationException(String message) {
        super(message);
    }
}
