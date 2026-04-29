# Audit Logging Examples

## Overview

Examples demonstrating the refined audit logging system with correct before/after semantics, transaction ID consistency, and async safety.

## Before/After Semantics

| Action | Before Field | After Field | Description |
|--------|--------------|-------------|-------------|
| CREATE | `null` | Created entity | No before state for new entities |
| UPDATE | Old entity | Updated entity | Captures state change |
| DELETE | Deleted entity | `null` | No after state for deleted entities |

## CREATE Example

### Service Method
```java
@CacheEvict(value = "demands", allEntries = true)
@Audit(module = AuditConstants.Modules.DEMAND, entity = "Demand", action = AuditConstants.Actions.CREATE)
public ResponseEntity<ApiResponse<?>> createDemand(CreateDemandDTO dto, Long id) {
    try {
        // Business logic to create demand
        Demand createdDemand = demandService.createDemandFromDTO(dto);
        return ResponseEntity.ok(ApiResponse.success("Demand created successfully", createdDemand));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(ApiResponse.error("Failed to create demand: " + e.getMessage()));
    }
}
```

### Audit Record Generated
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "module": "DEMAND",
  "entity": "Demand",
  "action": "CREATE",
  "user": "john.doe@company.com",
  "transactionId": "tx-12345",
  "requestId": "req-67890",
  "source": "API",
  "before": null,
  "after": {
    "demandId": "d123",
    "title": "Senior Developer",
    "priority": "HIGH",
    "status": "ACTIVE"
  },
  "timestamp": "2024-03-15T10:30:00"
}
```

## UPDATE Example

### Service Method
```java
@CacheEvict(value = "demands", allEntries = true)
@Audit(module = AuditConstants.Modules.DEMAND, entity = "Demand", action = AuditConstants.Actions.UPDATE)
public ResponseEntity<ApiResponse<?>> updateDemand(UpdateDemandDTO dto) {
    try {
        // Business logic to update demand
        Demand updatedDemand = demandService.updateDemandFromDTO(dto);
        return ResponseEntity.ok(ApiResponse.success("Demand updated successfully", updatedDemand));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(ApiResponse.error("Failed to update demand: " + e.getMessage()));
    }
}
```

### Audit Record Generated
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "module": "DEMAND",
  "entity": "Demand",
  "action": "UPDATE",
  "user": "jane.smith@company.com",
  "transactionId": "tx-12345",
  "requestId": "req-67891",
  "source": "API",
  "before": {
    "demandId": "d123",
    "title": "Senior Developer",
    "priority": "HIGH",
    "status": "ACTIVE"
  },
  "after": {
    "demandId": "d123",
    "title": "Lead Developer",
    "priority": "CRITICAL",
    "status": "ACTIVE"
  },
  "timestamp": "2024-03-15T11:45:00"
}
```

## DELETE Example

### Service Method
```java
@CacheEvict(value = "demands", allEntries = true)
@Audit(module = AuditConstants.Modules.DEMAND, entity = "Demand", action = AuditConstants.Actions.DELETE)
public ResponseEntity<ApiResponse<?>> deleteDemand(UUID demandId) {
    try {
        // Business logic to delete demand
        demandService.deleteDemandById(demandId);
        return ResponseEntity.ok(ApiResponse.success("Demand deleted successfully", null));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(ApiResponse.error("Failed to delete demand: " + e.getMessage()));
    }
}
```

### Audit Record Generated
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440002",
  "module": "DEMAND",
  "entity": "Demand",
  "action": "DELETE",
  "user": "admin@company.com",
  "transactionId": "tx-12345",
  "requestId": "req-67892",
  "source": "API",
  "before": {
    "demandId": "d123",
    "title": "Lead Developer",
    "priority": "CRITICAL",
    "status": "ACTIVE"
  },
  "after": null,
  "timestamp": "2024-03-15T12:15:00"
}
```

## Async Flow with Transaction ID Reuse

### Controller Method (Entry Point)
```java
@RestController
public class DemandController {
    
    @PostMapping("/demands/bulk-update")
    public ResponseEntity<ApiResponse<?>> bulkUpdateDemands(@RequestBody List<UpdateDemandDTO> dtos) {
        // Generate transaction ID for the entire flow
        TransactionContext.generateAndSetTransactionId();
        TransactionContext.setSource("API");
        
        try {
            // Process updates asynchronously
            demandService.bulkUpdateDemandsAsync(dtos);
            return ResponseEntity.ok(ApiResponse.success("Bulk update initiated", null));
        } finally {
            TransactionContext.clearIfPresent();
        }
    }
}
```

### Async Service Method
```java
@Service
public class DemandServiceImpl {
    
    @Async
    public void bulkUpdateDemandsAsync(List<UpdateDemandDTO> dtos) {
        // Set async context - transaction ID is already set from controller
        auditService.setAsyncContext();
        
        for (UpdateDemandDTO dto : dtos) {
            try {
                // This will reuse the transaction ID from the controller
                updateDemand(dto);
            } catch (Exception e) {
                log.error("Failed to update demand {}: {}", dto.getDemandId(), e.getMessage());
            }
        }
    }
}
```

### Audit Records Generated
```json
// First update in async flow
{
  "id": "550e8400-e29b-41d4-a716-446655440003",
  "module": "DEMAND",
  "entity": "Demand",
  "action": "UPDATE",
  "user": "john.doe@company.com",
  "transactionId": "tx-12345", // Same transaction ID for all updates
  "requestId": "req-67893",
  "source": "ASYNC",
  "before": { /* old state */ },
  "after": { /* new state */ },
  "timestamp": "2024-03-15T13:00:00"
}

// Second update in async flow
{
  "id": "550e8400-e29b-41d4-a716-446655440004",
  "module": "DEMAND",
  "entity": "Demand",
  "action": "UPDATE",
  "user": "john.doe@company.com",
  "transactionId": "tx-12345", // Same transaction ID
  "requestId": "req-67894",
  "source": "ASYNC",
  "before": { /* old state */ },
  "after": { /* new state */ },
  "timestamp": "2024-03-15T13:01:00"
}
```

## Manual Transaction ID Passing (Alternative Async Pattern)

### Service Method with Manual Transaction ID
```java
@Service
public class DemandServiceImpl {
    
    @Async
    public void updateDemandAsync(UpdateDemandDTO dto, String transactionId) {
        // Manually pass transaction ID for async context
        TransactionContext.setTransactionId(transactionId);
        TransactionContext.setSource("ASYNC");
        
        try {
            updateDemand(dto);
        } finally {
            TransactionContext.clearIfPresent();
        }
    }
}
```

### Controller Usage
```java
@PostMapping("/demands/{id}/update-async")
public ResponseEntity<ApiResponse<?>> updateDemandAsync(@PathVariable UUID id, @RequestBody UpdateDemandDTO dto) {
    String transactionId = TransactionContext.getOrCreateTransactionId();
    
    // Pass transaction ID to async method
    demandService.updateDemandAsync(dto, transactionId);
    
    return ResponseEntity.ok(ApiResponse.success("Async update initiated", null));
}
```

## Payload Size Control Examples

### Normal Payload (Within Limits)
```json
{
  "before": {
    "demandId": "d123",
    "title": "Senior Developer",
    "priority": "HIGH",
    "status": "ACTIVE"
  },
  "after": {
    "demandId": "d123",
    "title": "Lead Developer", 
    "priority": "CRITICAL",
    "status": "ACTIVE"
  }
}
```

### Truncated Payload (Exceeds 10KB Limit)
```json
{
  "before": {
    "truncated": true,
    "preview": "Demand{demandId=d123, title=Senior Developer, priority=HIGH, status=ACTIVE, description=Very long description that exceeds size limit...",
    "type": "Demand"
  },
  "after": {
    "truncated": true,
    "preview": "Demand{demandId=d123, title=Lead Developer, priority=CRITICAL, status=ACTIVE, description=Very long description that exceeds size limit...",
    "type": "Demand"
  }
}
```

### Fallback Payload (Serialization Failed)
```json
{
  "before": {
    "type": "Demand",
    "toString": "Demand@d123e4567-89ab-cdef-0123456789ab"
  },
  "after": {
    "type": "Demand",
    "toString": "Demand@d123e4567-89ab-cdef-0123456789ab"
  }
}
```

## Error Handling Example

### Method with Exception
```java
@Audit(module = AuditConstants.Modules.DEMAND, entity = "Demand", action = AuditConstants.Actions.UPDATE)
public ResponseEntity<ApiResponse<?>> updateDemand(UpdateDemandDTO dto) {
    try {
        // Business logic that throws exception
        validateDemandData(dto);
        Demand updated = demandService.updateDemandFromDTO(dto);
        return ResponseEntity.ok(ApiResponse.success("Demand updated", updated));
    } catch (ValidationException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed: " + e.getMessage()));
    }
}
```

### Audit Record for Failed Operation
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440005",
  "module": "DEMAND",
  "entity": "Demand",
  "action": "UPDATE",
  "user": "john.doe@company.com",
  "transactionId": "tx-12345",
  "requestId": "req-67895",
  "source": "API",
  "before": { /* old state or null */ },
  "after": "ERROR: Validation failed: Priority cannot be empty",
  "timestamp": "2024-03-15T14:30:00"
}
```

## Async Safety Warning

### Log Output for Missing Transaction ID
```
2024-03-15 15:00:00 WARN  [Async-1] AuditService: Audit triggered in async context without transaction_id. Consider manual transaction ID passing.
```

### Audit Record with Warning Context
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440006",
  "module": "DEMAND",
  "entity": "Demand", 
  "action": "UPDATE",
  "user": "system@company.com",
  "transactionId": null, // Missing transaction ID
  "requestId": "req-67896",
  "source": "ASYNC", // Source indicates async context
  "before": { /* old state */ },
  "after": { /* new state */ },
  "timestamp": "2024-03-15T15:00:00"
}
```

## Key Takeaways

1. **CREATE Operations**: `before = null`, `after = created entity`
2. **UPDATE Operations**: `before = old state`, `after = updated entity`
3. **DELETE Operations**: `before = deleted entity`, `after = null`
4. **Transaction IDs**: Reuse existing IDs, auto-generate only if needed
5. **Async Safety**: Set source to "ASYNC", log warnings for missing transaction IDs
6. **Payload Control**: Simplified objects with key fields, graceful truncation
7. **Error Handling**: Capture error messages in `after` field, never break business flow

The refined system provides consistent, production-safe audit logging while maintaining simplicity and developer productivity.
