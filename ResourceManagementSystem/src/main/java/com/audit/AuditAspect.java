package com.audit;

import com.service_imple.audit_service_impl.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditService auditService;

    @Around("@annotation(audit)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        String module = audit.module();
        String entity = audit.entity();
        String action = audit.action();
        
        // Initialize context only if not already set
        initializeContext();
        
        Object before = null;
        Object after = null;
        boolean success = false;
        
        try {
            // Check async safety
            auditService.checkAsyncSafety();
            
            // Apply correct before/after semantics
            before = captureBeforeState(joinPoint, action);
            
            // Execute method
            Object result = joinPoint.proceed();
            
            // Apply correct after semantics
            after = captureAfterState(result, action);
            success = true;
            
            return result;
        } catch (Exception e) {
            after = "ERROR: " + e.getMessage();
            throw e;
        } finally {
            // Only audit if method executed successfully or with known error
            if (success || after != null) {
                auditService.logAudit(module, entity, action, before, after);
            }
            
            // Cleanup context
            TransactionContext.clearIfPresent();
        }
    }

    private void initializeContext() {
        // Only generate transaction ID if none exists
        if (!TransactionContext.hasTransactionId()) {
            TransactionContext.getOrCreateTransactionId();
        }
        
        // Generate request ID if none exists
        if (TransactionContext.getRequestId() == null) {
            TransactionContext.getOrCreateRequestId();
        }
        
        // Set default source if none exists
        if (TransactionContext.getSource() == null) {
            TransactionContext.setSource("API");
        }
    }

    private Object captureBeforeState(ProceedingJoinPoint joinPoint, String action) {
        switch (action) {
            case "CREATE":
                // CREATE operations should have before = null
                return null;
            case "DELETE":
                // DELETE operations capture the entity being deleted
                return extractFirstArgument(joinPoint);
            case "UPDATE":
                // UPDATE operations try to capture old state
                return captureOldState(joinPoint);
            default:
                // For other actions, capture input parameters
                return extractRelevantData(joinPoint);
        }
    }

    private Object captureAfterState(Object result, String action) {
        switch (action) {
            case "DELETE":
                // DELETE operations should have after = null
                return null;
            default:
                // For CREATE, UPDATE, and others, capture the result
                return extractResultData(result);
        }
    }

    private Object captureOldState(ProceedingJoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args.length == 0) {
                return null;
            }
            
            Object firstArg = args[0];
            String entityId = extractEntityId(firstArg);
            
            if (entityId != null) {
                // Simple approach: try to find repository by convention
                String entityClassName = firstArg.getClass().getSimpleName();
                String repositoryName = entityClassName.toLowerCase() + "Repository";
                
                // For now, return null to skip complex repository discovery
                // This keeps the implementation simple as requested
                log.debug("Skipping before state capture for {} - repository lookup not implemented", entityClassName);
                return null;
            }
            
            return null; // Skip if we can't extract ID
        } catch (Exception e) {
            log.debug("Failed to capture old state: {}", e.getMessage());
            return null; // Graceful fallback
        }
    }

    private String extractEntityId(Object entity) {
        if (entity == null) {
            return null;
        }
        
        try {
            // Try common ID field names
            String[] idFieldNames = {"id", "Id", "ID", "uuid", "UUID"};
            
            for (String fieldName : idFieldNames) {
                try {
                    Field field = entity.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object idValue = field.get(entity);
                    if (idValue != null) {
                        return idValue.toString();
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    // Try next field name
                }
            }
            
            // Try common getter methods
            String[] getterNames = {"getId", "getUuid", "getUUID"};
            for (String getterName : getterNames) {
                try {
                    Method getter = entity.getClass().getMethod(getterName);
                    Object idValue = getter.invoke(entity);
                    if (idValue != null) {
                        return idValue.toString();
                    }
                } catch (Exception e) {
                    // Try next getter
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract entity ID: {}", e.getMessage());
        }
        
        return null;
    }

    private Object extractFirstArgument(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        return args.length > 0 ? args[0] : null;
    }

    private Object extractRelevantData(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length == 0) {
            return null;
        }
        if (args.length == 1) {
            return args[0];
        }
        return args;
    }

    private Object extractResultData(Object result) {
        return result;
    }
}
