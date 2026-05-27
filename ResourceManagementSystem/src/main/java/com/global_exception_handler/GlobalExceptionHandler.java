package com.global_exception_handler;

import com.dto.centralised_dto.ApiResponse;
import jakarta.persistence.PersistenceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Demand domain ─────────────────────────────────────────────────────────
    @ExceptionHandler(DemandExceptionHandler.class)
    public ResponseEntity<ApiResponse<?>> handleDemandException(DemandExceptionHandler ex) {
        log.warn("Demand exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildDomainResponse(ex.getStatus(), ex.getErrorCode(), ex.getMessage());
    }

    // ── Allocation domain ─────────────────────────────────────────────────────
    @ExceptionHandler(AllocationExceptionHandler.class)
    public ResponseEntity<ApiResponse<?>> handleAllocationException(AllocationExceptionHandler ex) {
        log.warn("Allocation exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildDomainResponse(ex.getStatus(), ex.getErrorCode(), ex.getMessage());
    }

    // ── Role-off domain ───────────────────────────────────────────────────────
    @ExceptionHandler(RoleOffExceptionHandler.class)
    public ResponseEntity<ApiResponse<?>> handleRoleOffException(RoleOffExceptionHandler ex) {
        log.warn("RoleOff exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildDomainResponse(ex.getStatus(), ex.getErrorCode(), ex.getMessage());
    }

    // ── Availability / bench domain ───────────────────────────────────────────
    @ExceptionHandler(AvailabilityExceptionHandler.class)
    public ResponseEntity<ApiResponse<?>> handleAvailabilityException(AvailabilityExceptionHandler ex) {
        log.warn("Availability exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildDomainResponse(ex.getStatus(), ex.getErrorCode(), ex.getMessage());
    }

    // ── Client / company domain ───────────────────────────────────────────────
    @ExceptionHandler(ClientExceptionHandler.class)
    public ResponseEntity<ApiResponse<?>> handleClientException(ClientExceptionHandler ex) {
        log.warn("Client exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildDomainResponse(ex.getStatus(), ex.getErrorCode(), ex.getMessage());
    }

    // ── Skill / certificate / taxonomy domain ─────────────────────────────────
    @ExceptionHandler(SkillExceptionHandler.class)
    public ResponseEntity<ApiResponse<?>> handleSkillException(SkillExceptionHandler ex) {
        log.warn("Skill exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildDomainResponse(ex.getStatus(), ex.getErrorCode(), ex.getMessage());
    }

    // ── Project / resource domain ─────────────────────────────────────────────
    @ExceptionHandler(ProjectExceptionHandler.class)
    public ResponseEntity<ApiResponse<?>> handleProjectException(ProjectExceptionHandler ex) {
        log.warn("Project exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildDomainResponse(ex.getStatus(), ex.getErrorCode(), ex.getMessage());
    }

    // ── CDC domain ────────────────────────────────────────────────────────────
    @ExceptionHandler(CDCExceptionHandler.class)
    public ResponseEntity<ApiResponse<?>> handleCdcException(CDCExceptionHandler ex) {
        log.warn("CDC exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildDomainResponse(ex.getStatus(), ex.getErrorCode(), ex.getMessage());
    }

    // ── Security domain (application-level security violations) ──────────────
    @ExceptionHandler(SecurityExceptionHandler.class)
    public ResponseEntity<ApiResponse<?>> handleSecurityDomainException(SecurityExceptionHandler ex) {
        log.warn("Security exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildDomainResponse(ex.getStatus(), ex.getErrorCode(), ex.getMessage());
    }

    // ── Bean-validation errors (@Valid / @Validated) ──────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage()
                ));
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed", errors));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<?>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed",
                Map.of(ex.getName(), "Invalid value")));
    }

    // ── Spring Security authentication / authorization ────────────────────────
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Forbidden"));
    }

    // ── External API errors ───────────────────────────────────────────────────
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpClientErrorException(HttpClientErrorException ex) {
        String message;
        if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            message = "External API authentication failed. Please check credentials.";
        } else if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
            message = "Access denied to external API.";
        } else {
            message = "External API error: " + ex.getMessage();
        }
        return ResponseEntity.status(ex.getStatusCode()).body(ApiResponse.error(message));
    }

    // ── Shared response builder ───────────────────────────────────────────────
    private ResponseEntity<ApiResponse<?>> buildDomainResponse(
            HttpStatus status, String errorCode, String message) {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("errorCode", errorCode);
        errorDetails.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(status).body(ApiResponse.error(message, errorDetails));
    }

    // ── JSON parse / deserialization errors ──────────────────────────────────
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("JSON parse error: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Malformed JSON or unreadable request body: " + ex.getMostSpecificCause().getMessage()));
    }

    // ── Database / persistence errors ─────────────────────────────────────────
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<?>> handleDataAccessException(DataAccessException ex) {
        log.error("Database error: {}", ex.getMostSpecificCause().getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Database error: " + ex.getMostSpecificCause().getMessage()));
    }

    @ExceptionHandler(PersistenceException.class)
    public ResponseEntity<ApiResponse<?>> handlePersistenceException(PersistenceException ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        log.error("Persistence error: {}", cause.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Persistence error: " + cause.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<?>> handleRuntimeException(RuntimeException ex) {
        log.error("Unhandled runtime exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error: " + ex.getMessage()));
    }
}
