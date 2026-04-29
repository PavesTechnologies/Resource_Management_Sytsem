# Audit Logging System Implementation Guide

## Overview

A simple and maintainable audit logging system implemented using Spring AOP with minimal manual effort and no over-engineering.

## Components Created

### 1. @Audit Annotation
**Location**: `com.audit.Audit`
- Custom annotation for marking methods that require audit logging
- Parameters: `module`, `entity`, `action`
- Example: `@Audit(module="DEMAND", entity="Demand", action="CREATE")`

### 2. Audit Entity
**Location**: `com.entity.audit_entities.Audit`
- Row-based audit table with fields:
  - `id` (UUID)
  - `module` (e.g., CLIENT, ALLOCATION)
  - `entity` (e.g., Client, ClientSLA)
  - `action` (CREATE, UPDATE, DELETE)
  - `user` (from Spring Security context)
  - `transaction_id` (optional, nullable)
  - `before` (TEXT/JSON)
  - `after` (TEXT/JSON)
  - `timestamp`

### 3. AuditRepository
**Location**: `com.repo.audit_repo.AuditRepository`
- Standard Spring Data JPA repository
- Query methods for filtering by module, entity, user, transaction_id

### 4. TransactionContext
**Location**: `com.audit.TransactionContext`
- ThreadLocal holder for transaction_id
- Methods: `setTransactionId()`, `getTransactionId()`, `clear()`, `generateAndSetTransactionId()`

### 5. AuditService
**Location**: `com.service_imple.audit_service_impl.AuditService`
- Centralized service for saving audit records
- Fetches current user from Spring Security context
- Converts objects to JSON using ObjectMapper
- Wraps logic in try/catch (audit failure doesn't break main flow)

### 6. AuditAspect
**Location**: `com.audit.AuditAspect`
- AOP aspect with @Around advice
- Intercepts methods annotated with @Audit
- Captures method result (after state)
- Calls AuditService to save audit logs

## Database Schema

```sql
CREATE TABLE audit (
    id BINARY(16) PRIMARY KEY,
    module VARCHAR(50) NOT NULL,
    entity VARCHAR(100) NOT NULL,
    action VARCHAR(20) NOT NULL,
    user VARCHAR(255),
    transaction_id VARCHAR(36),
    before TEXT,
    after TEXT,
    timestamp TIMESTAMP NOT NULL,
    INDEX idx_module (module),
    INDEX idx_entity (entity),
    INDEX idx_user (user),
    INDEX idx_transaction_id (transaction_id),
    INDEX idx_timestamp (timestamp)
);
```

## Usage Examples

### Basic Usage
```java
@Audit(module = "DEMAND", entity = "Demand", action = "CREATE")
public ResponseEntity<ApiResponse<?>> createDemand(CreateDemandDTO dto, Long id) {
    // Your business logic here
    return ResponseEntity.ok(ApiResponse.success("Demand created successfully", result));
}
```

### With Transaction ID
```java
@RestController
public class DemandController {
    
    @PostMapping("/demands")
    public ResponseEntity<ApiResponse<?>> createDemand(@RequestBody CreateDemandDTO dto) {
        // Generate transaction ID for complex flows
        TransactionContext.generateAndSetTransactionId();
        
        try {
            return demandService.createDemand(dto, projectId);
        } finally {
            TransactionContext.clear();
        }
    }
}
```

### Different Actions
```java
@Audit(module = "CLIENT", entity = "Client", action = "CREATE")
public Client createClient(CreateClientDTO dto) { }

@Audit(module = "CLIENT", entity = "Client", action = "UPDATE")
public Client updateClient(Long clientId, UpdateClientDTO dto) { }

@Audit(module = "ALLOCATION", entity = "ResourceAllocation", action = "DELETE")
public void deleteAllocation(UUID allocationId) { }
```

## Key Features

### 1. **Simplicity**
- Developers only need to add @Audit annotation
- No manual auditService calls in business logic
- Clean separation of concerns

### 2. **Robustness**
- Audit failures don't break main application flow
- Comprehensive error handling and logging
- Graceful degradation when security context is unavailable

### 3. **Flexibility**
- Supports any module, entity, and action combination
- Optional transaction ID for complex flows
- JSON serialization for complex objects

### 4. **Performance**
- Asynchronous-friendly design
- Minimal overhead for audit operations
- Efficient ThreadLocal usage

## What's NOT Included (Intentionally)

- ❌ Field-level diff engine
- ❌ Event sourcing
- ❌ Multiple audit tables
- ❌ Advanced caching or batching
- ❌ Complex context propagation for async methods

## Best Practices

### 1. Module Naming
Use consistent module names:
- `CLIENT` for client-related operations
- `DEMAND` for demand management
- `ALLOCATION` for resource allocations
- `PROJECT` for project operations
- `RESOURCE` for resource management

### 2. Entity Naming
Use the actual entity class names:
- `Client` for client entities
- `Demand` for demand entities
- `ResourceAllocation` for allocations
- `Project` for projects

### 3. Action Types
Standard action types:
- `CREATE` for new entity creation
- `UPDATE` for entity modifications
- `DELETE` for entity removal
- `STATUS_CHANGE` for status updates

### 4. Transaction ID Usage
- Use for complex multi-step operations
- Generate at controller level for web requests
- Clear in finally blocks to prevent memory leaks

## Monitoring and Queries

### Find All Audits for a Module
```java
List<Audit> audits = auditRepository.findByModuleOrderByTimestampDesc("DEMAND");
```

### Find All Audits for a User
```java
List<Audit> audits = auditRepository.findByUserOrderByTimestampDesc("john.doe");
```

### Find All Audits for a Transaction
```java
List<Audit> audits = auditRepository.findByTransactionIdOrderByTimestampDesc("tx-12345");
```

## Integration with Existing Code

The audit system integrates seamlessly with existing Spring Boot applications:

1. **AOP Dependency**: Already included in `spring-boot-starter-aop`
2. **Jackson**: Already available for JSON serialization
3. **Spring Security**: Automatically integrates for user context
4. **JPA**: Uses existing entity management

## Example Output

When a method annotated with `@Audit(module="DEMAND", entity="Demand", action="CREATE")` is called:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "module": "DEMAND",
  "entity": "Demand",
  "action": "CREATE",
  "user": "john.doe@company.com",
  "transactionId": "tx-12345",
  "before": null,
  "after": "{\"demandId\":\"d123\",\"title\":\"Senior Developer\",\"priority\":\"HIGH\"}",
  "timestamp": "2024-03-15T10:30:00"
}
```

This implementation provides a solid foundation for audit logging while maintaining simplicity and developer productivity.
