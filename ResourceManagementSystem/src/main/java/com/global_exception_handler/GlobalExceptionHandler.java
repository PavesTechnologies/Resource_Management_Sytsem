package com.global_exception_handler;

import com.dto.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler{

    @ExceptionHandler(ClientExceptionHandler.class)
    public ResponseEntity<ApiResponse> handleClientException(ClientExceptionHandler e){
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setSuccess(false);
        apiResponse.setMessage(e.getMessage());
        apiResponse.setData(null);
        return ResponseEntity.badRequest().body(apiResponse);
    }
//    @ExceptionHandler(DataIntegrityViolationException.class)
//    public ResponseEntity<ApiResponse<?>> handleDataIntegrityViolation(
//            DataIntegrityViolationException e) {
//
//        ApiResponse<?> apiResponse = new ApiResponse<>();
//        apiResponse.setSuccess(false);
//
//        // Customize message for UNIQUE constraint
//        apiResponse.setMessage("Serial number already exists");
//
//        apiResponse.setData(null);
//        return ResponseEntity.badRequest().body(apiResponse);
//    }
//
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<ApiResponse> handleValidationExceptions(
//            MethodArgumentNotValidException ex) {
//
//        String errorMessage = ex.getBindingResult()
//                .getFieldErrors()
//                .stream()
//                .map(error -> {
//                    String fieldName = error.getField();
//                    String message = error.getDefaultMessage();
//                    // Convert field name to readable format
//                    String readableFieldName = fieldName.replaceAll("([a-z])([A-Z])", "$1 $2")
//                                                           .replaceAll("^([a-z])", "$1")
//                                                           .toLowerCase();
//                    readableFieldName = Character.toUpperCase(readableFieldName.charAt(0)) + readableFieldName.substring(1);
//                    return readableFieldName + ": " + message;
//                })
//                .collect(Collectors.joining(", "));
//
//        ApiResponse apiResponse = new ApiResponse();
//        apiResponse.setSuccess(false);
//        apiResponse.setMessage(errorMessage);
//        apiResponse.setData(null);
//
//        return ResponseEntity.badRequest().body(apiResponse);
//    }

    @ExceptionHandler(ProjectExceptionHandler.class)
    public ResponseEntity<?> handleProjectException(ProjectExceptionHandler ex) {

        return ResponseEntity
                .status(ex.getStatus())
                .body(Map.of(
                        "errorCode", ex.getErrorCode(),
                        "message", ex.getMessage(),
                        "timestamp", LocalDateTime.now()
                ));
    }
}
