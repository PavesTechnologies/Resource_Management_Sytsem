# EOS → RMS CDC Integration Implementation Guide

## Overview

This document describes the production-grade EOS → RMS CDC integration using Debezium + MySQL binlog architecture. The implementation follows the same resiliency, retry, multi-instance safety, duplicate resistance, operational behavior, and enterprise standards as the existing PMS → RMS CDC implementation.

## Architecture

### Shared CDC Platform + Isolated EOS Business Processing

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           SHARED CDC INFRASTRUCTURE                           │
├─────────────────────────────────────────────────────────────────────────────────┤
│ • DebeziumEngine              • CdcSafeExecutor                              │
│ • FailureRecorder             • CdcFailureRepository                         │
│ • DebeziumChangeDetector      • CdcValueConverter                            │
│ • ReflectionUtil              • ShedLockConfig                               │
│ • Redis Infrastructure         • Audit Logging                               │
│ • Event Publishing            • Scheduler Infrastructure                      │
│ • Retry Framework             • DLQ Framework                               │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                            ISOLATED EOS COMPONENTS                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│ • EosDebeziumConfig           • EosDebeziumRunner                           │
│ • EosCdcHandler               • EosCdcMappingRegistry                        │
│ • EosCdcScheduler             • EOS-specific Repositories                     │
│ • EOS-specific Entities        • EOS-specific Services                        │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## Key Features

### Enterprise-Grade Resilience
- **Restart-safe processing**: Independent offset storage for EOS
- **Crash-safe recovery**: Automatic recovery from failures
- **Duplicate resistance**: Idempotent processing with change detection
- **Retry handling**: Configurable retry with exponential backoff
- **Dead-letter handling**: Failed event storage and retry mechanism

### Multi-Instance Safety
- **Pessimistic locking**: Database-level locks prevent concurrent modifications
- **Distributed coordination**: ShedLock ensures single instance execution
- **Independent offsets**: No offset sharing between PMS and EOS
- **Isolated storage**: Separate schema history and offset files

### Operational Excellence
- **Observability**: Comprehensive logging and metrics
- **Health monitoring**: Automated health checks and alerts
- **Maintenance scheduling**: Automated cleanup and maintenance tasks
- **Configuration management**: Externalized configuration with validation

## Implementation Components

### 1. EOS Debezium Configuration (`EosDebeziumConfig`)

**Purpose**: Independent Debezium connector configuration for EOS database.

**Key Features**:
- Separate connector name: `eos-cdc`
- Independent offset file: `/eos-cdc/eos-offsets.dat`
- Separate schema history: `/eos-cdc/eos-schema-history.dat`
- Isolated server name: `eos_mysql`
- Independent topic prefix: `eos`

**Configuration Properties**:
```properties
# EOS Database Configuration
eos.cdc.database.type=mysql
eos.cdc.connector.class=io.debezium.connector.mysql.MySqlConnector
eos.cdc.database.hostname=localhost
eos.cdc.database.port=3306
eos.cdc.database.user=eos_user
eos.cdc.database.password=eos_password
eos.cdc.database.name=eos_database

# EOS Table Configuration
eos.cdc.database.include.list=eos_database
eos.cdc.table.include.list=eos_database.eos_entities

# EOS Storage Configuration
eos.cdc.base.directory=${user.home}/eos-cdc
eos.cdc.server.name=eos_mysql
eos.cdc.topic.prefix=eos
eos.cdc.connector.name=eos-cdc
```

### 2. EOS CDC Handler (`EosCdcHandler`)

**Purpose**: EOS-specific business logic for processing CDC events.

**Key Features**:
- **Safe Execution**: Uses `CdcSafeExecutor` for exception handling
- **Change Detection**: Leverages `DebeziumChangeDetector` for efficient processing
- **Field Mapping**: Uses `EosCdcMappingRegistry` for column-to-field mapping
- **Transaction Management**: REQUIRES_NEW propagation for isolation
- **Error Handling**: Comprehensive error logging and failure recording

**Processing Pipeline**:
1. **Event Validation**: Filter snapshot records, extract operation type
2. **Safe Execution**: Wrap processing in `CdcSafeExecutor`
3. **Entity Processing**: Handle INSERT/UPDATE/DELETE operations
4. **Change Detection**: Identify changed columns using Debezium before/after
5. **Field Mapping**: Apply EOS-specific column mappings
6. **Business Logic**: Execute EOS-specific business rules
7. **Audit & Persist**: Update timestamps and save changes
8. **Downstream Processing**: Trigger EOS-specific downstream processes

### 3. EOS Debezium Runner (`EosDebeziumRunner`)

**Purpose**: Independent lifecycle management for EOS Debezium engine.

**Key Features**:
- **Independent Engine**: Separate DebeziumEngine instance
- **Lifecycle Management**: Proper startup and shutdown handling
- **Thread Isolation**: Dedicated single-thread executor
- **Error Handling**: Graceful error handling and logging

### 4. EOS CDC Mapping Registry (`EosCdcMappingRegistry`)

**Purpose**: Column-to-field mapping configuration for EOS entities.

**Key Features**:
- **Type Safety**: Strong typing with enum support
- **Extensible**: Easy to add new mappings
- **Validation**: Built-in mapping validation
- **Documentation**: Self-documenting mapping structure

**Mapping Example**:
```java
EOS_TO_RMS.put("name",
    new ColumnMapping("name", "name", FieldType.STRING, null));

EOS_TO_RMS.put("status",
    new ColumnMapping("status", "status", FieldType.ENUM, EosStatus.class));

EOS_TO_RMS.put("created_at",
    new ColumnMapping("created_at", "createdAt", FieldType.LOCAL_DATE_TIME, null));
```

### 5. EOS CDC Scheduler (`EosCdcScheduler`)

**Purpose**: Automated maintenance and retry operations for EOS CDC.

**Key Features**:
- **Retry Logic**: Automatic retry of failed events with exponential backoff
- **Cleanup Operations**: Automated cleanup of old completed failures
- **Health Monitoring**: Periodic health checks and metrics collection
- **Maintenance Tasks**: Scheduled maintenance operations

**Schedule Configuration**:
- **Retry Failed Events**: Every 5 minutes
- **Cleanup Old Failures**: Daily at 2 AM
- **Health Check**: Every hour
- **Metrics Collection**: Every 15 minutes

## Configuration

### Application Properties

Add the following to `application.properties`:

```properties
# EOS CDC Configuration - Independent from PMS CDC
eos.cdc.database.type=mysql
eos.cdc.connector.class=io.debezium.connector.mysql.MySqlConnector
eos.cdc.database.hostname=localhost
eos.cdc.database.port=3306
eos.cdc.database.user=eos_user
eos.cdc.database.password=eos_password
eos.cdc.database.name=eos_database
eos.cdc.database.include.list=eos_database
eos.cdc.table.include.list=eos_database.eos_entities
eos.cdc.base.directory=${user.home}/eos-cdc
eos.cdc.server.name=eos_mysql
eos.cdc.topic.prefix=eos
eos.cdc.connector.name=eos-cdc
```

### Database Requirements

**EOS Database**:
- MySQL 5.7+ or MariaDB 10.2+
- Binary logging enabled
- Row-based binary logging format
- Full row images in binary log

**RMS Database**:
- EOS entity tables created
- CDC failure tracking table (reused from PMS)
- Proper indexes for performance

## Deployment Considerations

### Multi-Instance Deployment

1. **Offset Storage**: Each instance has independent offset files
2. **Schema History**: Separate schema history files prevent conflicts
3. **Database Locking**: Pessimistic locking ensures data consistency
4. **Distributed Locking**: ShedLock prevents duplicate scheduled task execution

### High Availability

1. **Graceful Shutdown**: Proper engine shutdown on application stop
2. **Error Recovery**: Automatic retry with exponential backoff
3. **Health Monitoring**: Continuous health checks and alerts
4. **Failover Support**: Automatic failover with state recovery

### Performance Optimization

1. **Batch Processing**: Efficient batch processing of changes
2. **Connection Pooling**: Optimized database connection pools
3. **Memory Management**: Proper memory usage and garbage collection
4. **Monitoring**: Comprehensive performance metrics

## Testing Strategy

### Unit Testing

1. **Configuration Testing**: Validate EOS Debezium configuration
2. **Handler Testing**: Test EOS CDC handler with mock events
3. **Mapping Testing**: Validate column-to-field mappings
4. **Scheduler Testing**: Test retry and cleanup logic

### Integration Testing

1. **End-to-End Testing**: Full CDC pipeline testing
2. **Multi-Instance Testing**: Test concurrent instance behavior
3. **Failure Testing**: Test error handling and recovery
4. **Performance Testing**: Load testing with high volume

### Operational Testing

1. **Restart Testing**: Test application restart scenarios
2. **Failover Testing**: Test instance failover scenarios
3. **Maintenance Testing**: Test scheduled maintenance tasks
4. **Monitoring Testing**: Test health checks and alerts

## Monitoring and Observability

### Key Metrics

1. **Processing Rate**: Events processed per minute
2. **Error Rate**: Failed events percentage
3. **Processing Latency**: Time from event to processing
4. **Queue Size**: Number of pending events

### Health Checks

1. **Engine Status**: Debezium engine health
2. **Database Connectivity**: Database connection health
3. **Failure Queue**: Failed event queue size
4. **Processing Lag**: Event processing delay

### Logging

1. **Event Processing**: Detailed event processing logs
2. **Error Logging**: Comprehensive error logging
3. **Performance Logging**: Performance-related logs
4. **Audit Logging**: Complete audit trail

## Troubleshooting

### Common Issues

1. **Database Connection**: Check EOS database connectivity
2. **Binary Logging**: Verify binary logging configuration
3. **Offset Issues**: Check offset file permissions and corruption
4. **Schema History**: Verify schema history file integrity

### Debugging Steps

1. **Check Logs**: Review application and Debezium logs
2. **Validate Configuration**: Verify EOS CDC configuration
3. **Test Database**: Test EOS database connectivity
4. **Monitor Health**: Check health check endpoints

## Migration Strategy

### Phase 1: Infrastructure Setup
1. Create EOS CDC configuration
2. Set up EOS database connectivity
3. Create EOS entity tables
4. Configure monitoring and logging

### Phase 2: Basic CDC
1. Implement basic EOS CDC handler
2. Set up EOS Debezium runner
3. Test with sample data
4. Validate change detection

### Phase 3: Business Logic
1. Implement EOS-specific business logic
2. Add field mappings
3. Implement downstream processing
4. Test end-to-end scenarios

### Phase 4: Production Readiness
1. Add comprehensive error handling
2. Implement retry and recovery
3. Set up monitoring and alerts
4. Performance optimization

## Security Considerations

### Database Security
1. **Encrypted Connections**: Use SSL/TLS for database connections
2. **Access Control**: Principle of least privilege for database users
3. **Credential Management**: Secure credential storage and rotation

### Data Security
1. **Sensitive Data**: Handle sensitive data appropriately
2. **Data Masking**: Mask sensitive data in logs
3. **Audit Trail**: Complete audit trail for all changes

### Network Security
1. **Firewall Rules**: Proper firewall configuration
2. **Network Segmentation**: Isolate CDC traffic
3. **VPN/Tunneling**: Secure network connections

## Conclusion

The EOS → RMS CDC integration provides a production-grade, enterprise-ready solution for real-time data synchronization. Following the same architectural patterns as the existing PMS CDC implementation ensures consistency, reliability, and maintainability while providing isolation and independence for EOS-specific processing.

The implementation includes comprehensive error handling, retry mechanisms, multi-instance safety, and operational excellence features required for enterprise deployment.
