package com.exception.skill_exceptions;

public class DuplicateRoleExpectationException extends RuntimeException {
    
    public DuplicateRoleExpectationException(String message) {
        super(message);
    }
}
