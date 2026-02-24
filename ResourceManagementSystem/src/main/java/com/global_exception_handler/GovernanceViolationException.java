package com.global_exception_handler;

public class GovernanceViolationException extends RuntimeException {
    public GovernanceViolationException(String message) {
        super(message);
    }
}
