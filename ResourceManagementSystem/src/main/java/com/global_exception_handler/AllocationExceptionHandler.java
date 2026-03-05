package com.global_exception_handler;

public class AllocationExceptionHandler extends RuntimeException {
    public AllocationExceptionHandler(String message) {
        super(message);
    }
}
