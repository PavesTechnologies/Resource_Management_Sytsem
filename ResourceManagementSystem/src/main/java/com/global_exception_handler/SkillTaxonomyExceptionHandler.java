package com.global_exception_handler;

public class SkillTaxonomyExceptionHandler extends RuntimeException {
    
    public SkillTaxonomyExceptionHandler(String message) {
        super(message);
    }
    
    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
