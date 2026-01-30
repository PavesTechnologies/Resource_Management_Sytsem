package com.global_exception_handler;

import com.dto.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler{

    @ExceptionHandler(ClientException.class)
    public ResponseEntity<ApiResponse> handleClientException(ClientException e){
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setSuccess(false);
        apiResponse.setMessage(e.getMessage());
        apiResponse.setData(null);
        return ResponseEntity.badRequest().body(apiResponse);
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleDataIntegrityViolation(
            DataIntegrityViolationException e) {

        ApiResponse<?> apiResponse = new ApiResponse<>();
        apiResponse.setSuccess(false);

        // Customize message for UNIQUE constraint
        apiResponse.setMessage("Serial number already exists");

        apiResponse.setData(null);
        return ResponseEntity.badRequest().body(apiResponse);
    }
}
