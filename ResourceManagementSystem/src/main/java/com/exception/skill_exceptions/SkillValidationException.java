package com.exception.skill_exceptions;

public class SkillValidationException extends RuntimeException {
    
    public SkillValidationException(String message) {
        super(message);
    }
    
    public SkillValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
