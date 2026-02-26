package com.global_exception_handler;

public class CompanyExceptionHandler extends RuntimeException{
    private String ExceptionMessage;
    
    public CompanyExceptionHandler() {
        super();
    }
    
    public CompanyExceptionHandler(String message) {
        super(message);
        this.ExceptionMessage = message;
    }
    
    public CompanyExceptionHandler(String message, Throwable cause) {
        super(message, cause);
        this.ExceptionMessage = message;
    }
    
    public String getExceptionMessage() {
        return ExceptionMessage;
    }
}
