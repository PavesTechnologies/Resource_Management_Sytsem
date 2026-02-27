package com.global_exception_handler;

import com.dto.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import com.exception.skill_exceptions.SkillValidationException;
import com.exception.skill_exceptions.DuplicateRoleExpectationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> {
                    String fieldName = error.getField();
                    String message = error.getDefaultMessage();
                    // Convert field name to readable format
                    String readableFieldName = fieldName.replaceAll("([a-z])([A-Z])", "$1 $2")
                                                           .replaceAll("^([a-z])", "$1")
                                                           .toLowerCase();
                    readableFieldName = Character.toUpperCase(readableFieldName.charAt(0)) + readableFieldName.substring(1);
                    return readableFieldName + ": " + message;
                })
                .collect(Collectors.joining(", "));
        
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setSuccess(false);
        apiResponse.setMessage(errorMessage);
        apiResponse.setData(null);
        
        return ResponseEntity.badRequest().body(apiResponse);
    }

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

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse> handleAuthenticationException(AuthenticationException ex) {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setSuccess(false);
        apiResponse.setMessage("Authentication failed: " + ex.getMessage());
        apiResponse.setData(null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDeniedException(AccessDeniedException ex) {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setSuccess(false);
        apiResponse.setMessage("Access denied: " + ex.getMessage());
        apiResponse.setData(null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiResponse);
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ApiResponse> handleHttpClientErrorException(HttpClientErrorException ex) {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setSuccess(false);

        if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            apiResponse.setMessage("External API authentication failed. Please check credentials.");
        } else if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
            apiResponse.setMessage("Access denied to external API.");
        } else {
            apiResponse.setMessage("External API error: " + ex.getMessage());
        }

        apiResponse.setData(null);
        return ResponseEntity.status(ex.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(SkillTaxonomyExceptionHandler.class)
    public ResponseEntity<ApiResponse> handleSkillTaxonomyException(SkillTaxonomyExceptionHandler e) {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setSuccess(false);
        apiResponse.setMessage(e.getMessage());
        apiResponse.setData(null);
        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(CertificationComplianceException.class)
    public ResponseEntity<ApiResponse> handleCertificationException(
            CertificationComplianceException e) {

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setSuccess(false);
        apiResponse.setMessage(e.getMessage());
        apiResponse.setData(null);

        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(SkillValidationException.class)
    public ResponseEntity<ApiResponse> handleSkillValidationException(SkillValidationException e) {
        log.warn("Skill validation error: {}", e.getMessage());
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setSuccess(false);
        apiResponse.setMessage(e.getMessage());
        apiResponse.setData(null);
        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(DuplicateRoleExpectationException.class)
    public ResponseEntity<ApiResponse> handleDuplicateRoleExpectationException(DuplicateRoleExpectationException e) {
        log.warn("Duplicate role expectation: {}", e.getMessage());
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setSuccess(false);
        apiResponse.setMessage(e.getMessage());
        apiResponse.setData(null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiResponse);
    }

}
