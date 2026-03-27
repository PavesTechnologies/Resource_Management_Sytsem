# Resource Availability Ledger System - Database Schema Documentation

## Overview

The Resource Availability Ledger System uses a daily granular approach for tracking resource availability, with comprehensive event logging and dead letter queue support for enterprise-grade reliability.

## Tables

### 1. resource_availability_ledger_daily

**Purpose**: Stores daily availability calculations for each resource.

**Columns**:
```sql
CREATE TABLE resource_availability_ledger_daily (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_id BIGINT NOT NULL,
    date DATE NOT NULL,
    standard_hours INT NOT NULL,
    holiday_hours INT NOT NULL,
    leave_hours INT NOT NULL,
    confirmed_alloc_hours INT NOT NULL,
    draft_alloc_hours INT NOT NULL,
    total_allocation_percentage INT NOT NULL,
    available_percentage INT NOT NULL,
    is_overallocated BOOLEAN NOT NULL,
    over_allocation_percentage INT NOT NULL,
    availability_trust_flag BOOLEAN NOT NULL,
    calculation_version BIGINT NOT NULL,
    last_event_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    
    UNIQUE KEY uk_resource_date (resource_id, date),
    INDEX idx_resource_id (resource_id),
    INDEX idx_date (date),
    INDEX idx_resource_date (resource_id, date),
    INDEX idx_trust_flag (availability_trust_flag),
    INDEX idx_last_event (last_event_id),
    INDEX idx_updated_at (updated_at)
);
```

**Constraints**:
- `resource_id` references `resource.resource_id` (FK constraint should be added)
- `standard_hours` must be 0 or 8 (based on working day logic)
- `total_allocation_percentage` should be sum of confirmed + draft allocations
- `available_percentage` calculated as: `(firm_available_hours / standard_hours) * 100`

**Sample Data**:
```sql
INSERT INTO resource_availability_ledger_daily (
    resource_id, date, standard_hours, holiday_hours, leave_hours,
    confirmed_alloc_hours, draft_alloc_hours, total_allocation_percentage,
    available_percentage, is_overallocated, over_allocation_percentage,
    availability_trust_flag, calculation_version, last_event_id
) VALUES 
(1001, '2026-03-26', 8, 0, 0, 6, 2, 100, 25, false, 0, true, 1711430400000, 'LEDGER-ABC123'),
(1001, '2026-03-27', 8, 0, 0, 8, 0, 100, 0, false, 0, true, 1711430400000, 'LEDGER-ABC123'),
(1002, '2026-03-26', 8, 8, 0, 0, 0, 0, 0, false, 0, true, 1711430400000, 'LEDGER-DEF456');
```

### 2. ledger_event_log

**Purpose**: Tracks all ledger events for idempotency and auditing.

**Columns**:
```sql
CREATE TABLE ledger_event_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL UNIQUE,
    resource_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_hash VARCHAR(64) NOT NULL,
    processed_flag BOOLEAN NOT NULL DEFAULT FALSE,
    retry_count INT NOT NULL DEFAULT 0,
    status ENUM('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'RETRY_EXHAUSTED') NOT NULL,
    error_message TEXT,
    processing_started_at TIMESTAMP NULL,
    processing_completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_event_id (event_id),
    INDEX idx_resource_id (resource_id),
    INDEX idx_event_type (event_type),
    INDEX idx_event_hash (event_hash),
    INDEX idx_processed_flag (processed_flag),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);
```

**Sample Data**:
```sql
INSERT INTO ledger_event_log (
    event_id, resource_id, event_type, event_hash, processed_flag,
    retry_count, status, processing_started_at, processing_completed_at
) VALUES 
('LEDGER-ABC123', 1001, 'ALLOCATION_CHANGED', '1234567890', TRUE, 0, 'SUCCESS', '2026-03-26 10:00:00', '2026-03-26 10:00:05'),
('LEDGER-DEF456', 1002, 'ROLE_OFF', '2345678901', TRUE, 1, 'SUCCESS', '2026-03-26 10:01:00', '2026-03-26 10:01:03'),
('LEDGER-GHI789', 1003, 'RESOURCE_CREATED', '3456789012', FALSE, 0, 'FAILED', '2026-03-26 10:02:00', NULL);
```

### 3. dead_letter_queue

**Purpose**: Stores failed events for retry and manual processing.

**Columns**:
```sql
CREATE TABLE dead_letter_queue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    max_retry_count INT NOT NULL DEFAULT 3,
    status ENUM('PENDING_RETRY', 'RETRY_EXHAUSTED', 'MANUALLY_PROCESSED', 'PERMANENTLY_FAILED') NOT NULL,
    next_retry_at TIMESTAMP NULL,
    last_retry_at TIMESTAMP NULL,
    original_event_type VARCHAR(50),
    resource_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_event_id (event_id),
    INDEX idx_status (status),
    INDEX idx_retry_count (retry_count),
    INDEX idx_created_at (created_at),
    INDEX idx_next_retry_at (next_retry_at)
);
```

**Sample Data**:
```sql
INSERT INTO dead_letter_queue (
    event_id, payload, error_message, retry_count, max_retry_count,
    status, next_retry_at, original_event_type, resource_id
) VALUES 
('LEDGER-GHI789', '{"eventId":"LEDGER-GHI789","resourceId":1003,...}', 
 'API timeout', 2, 3, 'PENDING_RETRY', '2026-03-26 10:17:00', 'RESOURCE_CREATED', 1003),
('LEDGER-JKL012', '{"eventId":"LEDGER-JKL012","resourceId":1004,...}', 
 'Invalid data format', 3, 3, 'RETRY_EXHAUSTED', NULL, 'ALLOCATION_CHANGED', 1004);
```

## Common Query Patterns

### 1. Resource Availability Queries

**Get availability for a specific date range**:
```sql
SELECT 
    date,
    standard_hours,
    holiday_hours,
    leave_hours,
    confirmed_alloc_hours,
    draft_alloc_hours,
    total_allocation_percentage,
    available_percentage,
    is_overallocated,
    availability_trust_flag
FROM resource_availability_ledger_daily 
WHERE resource_id = 1001 
    AND date BETWEEN '2026-03-01' AND '2026-03-31'
ORDER BY date;
```

**Get overallocated resources for a specific date**:
```sql
SELECT 
    r.resource_id,
    r.employee_id,
    r.first_name,
    r.last_name,
    l.total_allocation_percentage,
    l.over_allocation_percentage
FROM resource_availability_ledger_daily l
JOIN resource r ON l.resource_id = r.resource_id
WHERE l.date = '2026-03-26' 
    AND l.is_overallocated = TRUE
    AND l.standard_hours > 0;
```

**Get availability summary for a resource**:
```sql
SELECT 
    COUNT(*) as total_days,
    SUM(CASE WHEN standard_hours > 0 THEN 1 ELSE 0 END) as working_days,
    SUM(CASE WHEN available_percentage > 0 THEN 1 ELSE 0 END) as available_days,
    SUM(CASE WHEN is_overallocated THEN 1 ELSE 0 END) as overallocated_days,
    ROUND(AVG(CASE WHEN standard_hours > 0 THEN available_percentage ELSE 0 END), 2) as avg_availability
FROM resource_availability_ledger_daily
WHERE resource_id = 1001 
    AND date BETWEEN '2026-03-01' AND '2026-03-31';
```

### 2. Event Monitoring Queries

**Get recent events for a resource**:
```sql
SELECT 
    event_id,
    event_type,
    status,
    retry_count,
    created_at,
    processing_completed_at,
    TIMESTAMPDIFF(SECOND, processing_started_at, processing_completed_at) as processing_time_seconds
FROM ledger_event_log
WHERE resource_id = 1001
    AND created_at >= '2026-03-26 00:00:00'
ORDER BY created_at DESC;
```

**Get failed events needing attention**:
```sql
SELECT 
    event_id,
    resource_id,
    event_type,
    retry_count,
    error_message,
    created_at,
    updated_at
FROM ledger_event_log
WHERE status IN ('FAILED', 'RETRY_EXHAUSTED')
    AND created_at >= '2026-03-25 00:00:00'
ORDER BY created_at DESC;
```

**Event processing statistics**:
```sql
SELECT 
    event_type,
    status,
    COUNT(*) as event_count,
    TIMESTAMPDIFF(SECOND, MIN(processing_started_at), MAX(processing_completed_at)) as total_processing_time
FROM ledger_event_log
WHERE created_at BETWEEN '2026-03-26 00:00:00' AND '2026-03-26 23:59:59'
    AND processing_started_at IS NOT NULL
GROUP BY event_type, status
ORDER BY event_type, status;
```

### 3. Dead Letter Queue Queries

**Get events ready for retry**:
```sql
SELECT 
    id,
    event_id,
    original_event_type,
    resource_id,
    retry_count,
    max_retry_count,
    error_message,
    next_retry_at
FROM dead_letter_queue
WHERE status = 'PENDING_RETRY' 
    AND next_retry_at <= NOW()
ORDER BY next_retry_at ASC;
```

**DLQ statistics by event type**:
```sql
SELECT 
    original_event_type,
    status,
    COUNT(*) as count,
    AVG(retry_count) as avg_retries
FROM dead_letter_queue
WHERE created_at >= '2026-03-25 00:00:00'
GROUP BY original_event_type, status
ORDER BY original_event_type, status;
```

### 4. Performance Optimization Queries

**Identify untrustworthy entries**:
```sql
SELECT 
    resource_id,
    COUNT(*) as untrustworthy_days,
    MIN(date) as first_untrustworthy_date,
    MAX(date) as last_untrustworthy_date
FROM resource_availability_ledger_daily
WHERE availability_trust_flag = FALSE
    AND date >= '2026-03-01'
GROUP BY resource_id
HAVING COUNT(*) > 0
ORDER BY COUNT(*) DESC;
```

**Batch update trust flags**:
```sql
UPDATE resource_availability_ledger_daily
SET availability_trust_flag = FALSE, updated_at = NOW()
WHERE resource_id = 1001 
    AND date BETWEEN '2026-03-01' AND '2026-03-31';
```

**Cleanup old events**:
```sql
DELETE FROM ledger_event_log 
WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY) 
    AND status = 'SUCCESS';

DELETE FROM dead_letter_queue 
WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY) 
    AND status IN ('MANUALLY_PROCESSED', 'PERMANENTLY_FAILED');
```

## Index Optimization

### Recommended Indexes

**For resource_availability_ledger_daily**:
```sql
-- Primary lookup index
CREATE INDEX idx_resource_date_lookup ON resource_availability_ledger_daily(resource_id, date);

-- For availability queries
CREATE INDEX idx_availability_query ON resource_availability_ledger_daily(date, available_percentage);

-- For overallocation detection
CREATE INDEX idx_overallocation ON resource_availability_ledger_daily(date, is_overallocated) 
WHERE is_overallocated = TRUE;

-- For trust flag queries
CREATE INDEX idx_trust_filter ON resource_availability_ledger_daily(availability_trust_flag, date)
WHERE availability_trust_flag = FALSE;
```

**For ledger_event_log**:
```sql
-- For event processing
CREATE INDEX idx_event_processing ON ledger_event_log(status, created_at, event_id);

-- For resource event history
CREATE INDEX idx_resource_events ON ledger_event_log(resource_id, created_at DESC);

-- For duplicate detection
CREATE INDEX idx_event_hash_lookup ON ledger_event_log(event_hash, event_id);
```

## Partitioning Strategy

For large datasets, consider partitioning by date:

```sql
-- Partition resource_availability_ledger_daily by month
ALTER TABLE resource_availability_ledger_daily 
PARTITION BY RANGE (YEAR(date) * 100 + MONTH(date)) (
    PARTITION p202601 VALUES LESS THAN (202602),
    PARTITION p202602 VALUES LESS THAN (202603),
    PARTITION p202603 VALUES LESS THAN (202604),
    -- ... more partitions
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- Partition ledger_event_log by month
ALTER TABLE ledger_event_log 
PARTITION BY RANGE (YEAR(created_at) * 100 + MONTH(created_at)) (
    PARTITION p202601 VALUES LESS THAN (202602),
    PARTITION p202602 VALUES LESS THAN (202603),
    PARTITION p202603 VALUES LESS THAN (202604),
    -- ... more partitions
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

## Data Archival Strategy

```sql
-- Archive old ledger entries (older than 2 years)
CREATE TABLE resource_availability_ledger_daily_archive LIKE resource_availability_ledger_daily;

INSERT INTO resource_availability_ledger_daily_archive 
SELECT * FROM resource_availability_ledger_daily 
WHERE date < DATE_SUB(NOW(), INTERVAL 2 YEAR);

DELETE FROM resource_availability_ledger_daily 
WHERE date < DATE_SUB(NOW(), INTERVAL 2 YEAR);
```

## Monitoring and Maintenance

### Daily Health Checks

```sql
-- Check for processing delays
SELECT 
    'Processing Delay' as metric,
    COUNT(*) as count,
    MIN(created_at) as oldest_event
FROM ledger_event_log
WHERE status = 'PENDING' 
    AND created_at < DATE_SUB(NOW(), INTERVAL 1 HOUR);

-- Check DLQ backlog
SELECT 
    'DLQ Backlog' as metric,
    COUNT(*) as count,
    MAX(retry_count) as max_retries
FROM dead_letter_queue
WHERE status = 'PENDING_RETRY';

-- Check data consistency
SELECT 
    'Data Consistency' as metric,
    COUNT(*) as inconsistent_entries
FROM resource_availability_ledger_daily
WHERE calculation_version = 0 
    OR availability_trust_flag IS NULL;
```

### Performance Metrics

```sql
-- Event processing performance
SELECT 
    DATE(created_at) as processing_date,
    event_type,
    AVG(TIMESTAMPDIFF(SECOND, processing_started_at, processing_completed_at)) as avg_processing_time_seconds,
    COUNT(*) as event_count
FROM ledger_event_log
WHERE processing_started_at IS NOT NULL 
    AND processing_completed_at IS NOT NULL
    AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY DATE(created_at), event_type
ORDER BY processing_date DESC, event_type;
```

## Security Considerations

1. **Row-Level Security**: Consider implementing RLS to restrict access to specific resources
2. **Audit Trail**: The ledger_event_log provides comprehensive audit capabilities
3. **Data Encryption**: Sensitive resource data should be encrypted at rest
4. **Access Controls**: Implement proper database user permissions for different operations

## Backup and Recovery

1. **Incremental Backups**: Daily incremental backups for ledger tables
2. **Point-in-Time Recovery**: Enable binary logging for PITR capabilities
3. **Cross-Region Replication**: Consider read replicas for disaster recovery
4. **Validation Scripts**: Regular data consistency checks between primary and replicas
