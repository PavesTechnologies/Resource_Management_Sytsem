package com.service_imple.audit_service_impl;

import com.audit.TransactionContext;
import com.entity.audit_entities.Audit;
import com.repo.audit_repo.AuditRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    private static final int MAX_PAYLOAD_SIZE = 10000; // 10KB max payload

    @Transactional
    public void logAudit(String module, String entity, String action, Object before, Object after) {
        try {
            Audit audit = new Audit();
            audit.setModule(module);
            audit.setEntity(entity);
            audit.setAction(action);
            audit.setUser(getCurrentUser());
            audit.setTransactionId(TransactionContext.getTransactionId());
            audit.setRequestId(TransactionContext.getRequestId());
            audit.setSource(TransactionContext.getSource() != null ? TransactionContext.getSource() : "API");
            audit.setBefore(convertToJsonSafely(before));
            audit.setAfter(convertToJsonSafely(after));

            auditRepository.save(audit);
            log.debug("Audit log saved: {} {} {} for user: {}", module, entity, action, audit.getUser());
        } catch (Exception e) {
            log.error("Failed to save audit log for {} {} {}: {}", module, entity, action, e.getMessage(), e);
        }
    }

    public Object fetchBeforeState(Object repository, String entityId, String entityType) {
        if (repository == null || entityId == null || entityType == null) {
            return null;
        }
        
        try {
            Method findByIdMethod = repository.getClass().getMethod("findById", Object.class);
            Optional<?> result = (Optional<?>) findByIdMethod.invoke(repository, entityId);
            return result.orElse(null);
        } catch (Exception e) {
            log.debug("Failed to fetch before state for {} {}: {}", entityType, entityId, e.getMessage());
            return null;
        }
    }

    private String getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.debug("Failed to get current user from security context: {}", e.getMessage());
        }
        return "anonymous";
    }

    private String convertToJsonSafely(Object object) {
        if (object == null) {
            return null;
        }
        
        try {
            // Create a simplified version with only key fields
            Object simplified = simplifyObject(object);
            String json = objectMapper.writeValueAsString(simplified);
            
            if (json.length() > MAX_PAYLOAD_SIZE) {
                log.warn("Payload too large ({} chars), creating truncated preview", json.length());
                return createTruncatedJson(object);
            }
            
            return json;
        } catch (JsonProcessingException e) {
            log.warn("Failed to convert object to JSON: {}", e.getMessage());
            return createFallbackJson(object);
        }
    }

    private Object simplifyObject(Object object) {
        if (object == null) {
            return null;
        }
        
        // For simple types, return as-is
        if (object instanceof String || object instanceof Number || object instanceof Boolean) {
            return object;
        }
        
        // For entities, create a map with only key fields
        Map<String, Object> simplified = new HashMap<>();
        
        try {
            // Use reflection to get common fields
            java.lang.reflect.Field[] fields = object.getClass().getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(object);
                
                // Include only key fields (ID, name, status, etc.)
                if (isKeyField(field.getName()) && value != null) {
                    simplified.put(field.getName(), value);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to simplify object: {}", e.getMessage());
            return object; // Fallback to original object
        }
        
        return simplified.isEmpty() ? object : simplified;
    }

    private boolean isKeyField(String fieldName) {
        String lowerName = fieldName.toLowerCase();
        return lowerName.contains("id") || 
               lowerName.contains("name") || 
               lowerName.contains("title") || 
               lowerName.contains("status") || 
               lowerName.contains("type") || 
               lowerName.contains("code") ||
               lowerName.equals("email") ||
               lowerName.equals("priority");
    }

    private String createTruncatedJson(Object object) {
        Map<String, Object> truncated = new HashMap<>();
        truncated.put("truncated", true);
        truncated.put("preview", object.toString().substring(0, Math.min(500, object.toString().length())));
        truncated.put("type", object.getClass().getSimpleName());
        
        try {
            return objectMapper.writeValueAsString(truncated);
        } catch (JsonProcessingException e) {
            return "{\"truncated\":true,\"preview\":\"Unable to serialize\",\"type\":\"Unknown\"}";
        }
    }

    private String createFallbackJson(Object object) {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("type", object.getClass().getSimpleName());
        fallback.put("toString", object.toString().substring(0, Math.min(1000, object.toString().length())));
        
        try {
            return objectMapper.writeValueAsString(fallback);
        } catch (JsonProcessingException e) {
            return "{\"type\":\"Error\",\"toString\":\"Serialization failed\"}";
        }
    }

    public void checkAsyncSafety() {
        if (!TransactionContext.hasTransactionId()) {
            String source = TransactionContext.getSource();
            if ("ASYNC".equals(source)) {
                log.warn("Audit triggered in async context without transaction_id. Consider manual transaction ID passing.");
            }
        }
    }

    public void setAsyncContext() {
        if (TransactionContext.getSource() == null) {
            TransactionContext.setSource("ASYNC");
        }
    }
}
