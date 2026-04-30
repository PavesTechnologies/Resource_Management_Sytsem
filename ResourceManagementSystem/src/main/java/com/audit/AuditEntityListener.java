package com.audit;

import com.entity.audit_entities.Audit;
import com.repo.audit_repo.AuditRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.persistence.*;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEntityListener {

    private static AuditRepository auditRepository;
    private static ObjectMapper objectMapper;

    // Static setters for dependency injection
    public static void setAuditRepository(AuditRepository repo) {
        auditRepository = repo;
    }

    public static void setObjectMapper(ObjectMapper mapper) {
        objectMapper = mapper;
    }

    @PrePersist
    public void prePersist(Object entity) {
        logAudit(entity, "CREATE", null, entity);
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        Object oldState = captureOldState(entity);
        logAudit(entity, "UPDATE", oldState, entity);
    }

    @PreRemove
    public void preRemove(Object entity) {
        logAudit(entity, "DELETE", entity, null);
    }

    private void logAudit(Object entity, String action, Object before, Object after) {
        try {
            if (auditRepository == null) {
                log.warn("AuditRepository not initialized, skipping audit log");
                return;
            }

            Audit audit = new Audit();
            audit.setModule(getModuleName(entity));
            audit.setEntity(entity.getClass().getSimpleName());
            audit.setAction(action);
            audit.setUser(getCurrentUser());
            audit.setTransactionId(generateTransactionId());
            audit.setRequestId(generateRequestId());
            audit.setSource("DATABASE");
            audit.setBefore(convertToJsonSafely(before));
            audit.setAfter(convertToJsonSafely(after));

            auditRepository.save(audit);
            log.debug("Audit log saved: {} {} {} for user: {}", 
                    audit.getModule(), audit.getEntity(), audit.getAction(), audit.getUser());
        } catch (Exception e) {
            log.error("Failed to save audit log for {} {} {}: {}", 
                    getModuleName(entity), entity.getClass().getSimpleName(), action, e.getMessage(), e);
        }
    }

    private String getModuleName(Object entity) {
        String packageName = entity.getClass().getPackage().getName();
        if (packageName.contains("client")) return "CLIENT";
        if (packageName.contains("resource")) return "RESOURCE";
        if (packageName.contains("project")) return "PROJECT";
        if (packageName.contains("demand")) return "DEMAND";
        if (packageName.contains("allocation")) return "ALLOCATION";
        if (packageName.contains("roleoff")) return "ROLEOFF";
        if (packageName.contains("skill")) return "SKILL";
        if (packageName.contains("bench")) return "BENCH";
        return "UNKNOWN";
    }

    private Object captureOldState(Object entity) {
        try {
            Map<String, Object> oldState = new HashMap<>();
            Field[] fields = entity.getClass().getDeclaredFields();
            
            for (Field field : fields) {
                if (!field.getName().equals("serialVersionUID") && 
                    !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    if (value != null) {
                        oldState.put(field.getName(), value);
                    }
                }
            }
            return oldState.isEmpty() ? null : oldState;
        } catch (Exception e) {
            log.debug("Failed to capture old state: {}", e.getMessage());
            return null;
        }
    }

    private String getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                return auth.getName(); // Spring Security already extracted from JWT
            }
        } catch (Exception e) {
            log.debug("Failed to get current user: {}", e.getMessage());
        }
        return "SYSTEM";
    }

    private String generateTransactionId() {
        return "TXN-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    private String generateRequestId() {
        return "REQ-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    private String convertToJsonSafely(Object obj) {
        if (obj == null) return null;
        try {
            if (objectMapper != null) {
                return objectMapper.writeValueAsString(obj);
            }
        } catch (Exception e) {
            log.debug("Failed to convert to JSON: {}", e.getMessage());
        }
        return obj.toString();
    }
}
