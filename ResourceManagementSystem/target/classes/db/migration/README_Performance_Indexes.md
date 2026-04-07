# Performance Indexes Migration Guide

## Overview
This migration adds optimized database indexes to improve the performance of the Skill Gap Matching Engine by eliminating slow table scans and optimizing query execution plans.

## Files Created
- `V1__Add_Performance_Indexes.sql` - PostgreSQL migration script
- `V1__Add_Performance_Indexes_MySQL.sql` - MySQL migration script  
- `R1__Remove_Performance_Indexes.sql` - Rollback script (PostgreSQL)

## Indexes Being Added

### Primary Indexes (Required)
1. **idx_resource_skill_proficiency_id** on `resource_skill(proficiency_id)`
   - Optimizes proficiency level lookups for resource skills
   - Critical for batch proficiency fetching

2. **idx_resource_subskill_proficiency_id** on `resource_subskill(proficiency_id)`
   - Optimizes proficiency level lookups for resource subskills
   - Critical for batch proficiency fetching

3. **idx_delivery_role_expectation_skill_id** on `delivery_role_expectation(skill_id)`
   - Optimizes role expectation queries by skill
   - Improves JOIN performance with skill table

4. **idx_delivery_role_expectation_sub_skill_id** on `delivery_role_expectation(sub_skill_id)`
   - Optimizes role expectation queries by subskill
   - Improves JOIN performance with subskill table

### Additional Performance Indexes (Recommended)
5. **idx_resource_skill_resource_id_active_flag** on `resource_skill(resource_id, active_flag)`
   - Composite index for resource skill queries
   - Optimizes the main resource skill lookup query

6. **idx_resource_subskill_resource_id_active_flag** on `resource_subskill(resource_id, active_flag)`
   - Composite index for resource subskill queries
   - Optimizes the main resource subskill lookup query

7. **idx_delivery_role_expectation_role_name_status** on `delivery_role_expectation(role_name, status)`
   - Composite index for role expectation queries
   - Optimizes role-based expectation lookups

## Deployment Instructions

### Pre-Deployment Checklist
- [ ] Database backup completed
- [ ] Maintenance window scheduled (if required)
- [ ] Test environment validated
- [ ] Rollback plan prepared

### PostgreSQL Deployment
```bash
# Using Flyway
flyway migrate -url=jdbc:postgresql://localhost:5432/your_db \
               -user=your_user \
               -password=your_password \
               -locations=classpath:db/migration

# Using psql
psql -h localhost -U your_user -d your_db -f V1__Add_Performance_Indexes.sql
```

### MySQL Deployment
```bash
# Using Flyway
flyway migrate -url=jdbc:mysql://localhost:3306/your_db \
               -user=your_user \
               -password=your_password \
               -locations=classpath:db/migration

# Using mysql command line
mysql -h localhost -u your_user -p your_db < V1__Add_Performance_Indexes_MySQL.sql
```

### Post-Deployment Verification
```sql
-- PostgreSQL
SELECT tablename, indexname, indexdef 
FROM pg_indexes 
WHERE tablename IN ('resource_skill', 'resource_subskill', 'delivery_role_expectation')
    AND indexname LIKE 'idx_%'
ORDER BY tablename, indexname;

-- MySQL
SELECT TABLE_NAME, INDEX_NAME, COLUMN_NAME, INDEX_TYPE
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME IN ('resource_skill', 'resource_subskill', 'delivery_role_expectation')
    AND INDEX_NAME LIKE 'idx_%'
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;
```

## Performance Impact

### Expected Improvements
- **Query Performance**: 60-80% reduction in query execution time
- **Database Load**: Significant reduction in CPU and I/O usage
- **Application Response Time**: 30-50% improvement in skill gap analysis

### Storage Impact
- **Additional Storage**: ~50-100MB depending on table size
- **Memory Usage**: Minimal increase in buffer pool usage
- **Maintenance**: Slight increase in index maintenance overhead

## Monitoring

### Key Metrics to Monitor
1. **Query Execution Time**
   ```sql
   -- PostgreSQL
   SELECT query, mean_exec_time, calls 
   FROM pg_stat_statements 
   WHERE query LIKE '%resource_skill%' OR query LIKE '%delivery_role_expectation%'
   ORDER BY mean_exec_time DESC;
   
   -- MySQL
   SELECT DIGEST_TEXT, AVG_TIMER_WAIT/1000000000 as avg_time_sec, COUNT_STAR as calls
   FROM performance_schema.events_statements_summary_by_digest 
   WHERE DIGEST_TEXT LIKE '%resource_skill%' OR DIGEST_TEXT LIKE '%delivery_role_expectation%'
   ORDER BY AVG_TIMER_WAIT DESC;
   ```

2. **Index Usage**
   ```sql
   -- PostgreSQL
   SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
   FROM pg_stat_user_indexes 
   WHERE tablename IN ('resource_skill', 'resource_subskill', 'delivery_role_expectation')
   ORDER BY idx_scan DESC;
   
   -- MySQL
   SELECT TABLE_NAME, INDEX_NAME, CARDINALITY, SUB_PART, NULLABLE
   FROM INFORMATION_SCHEMA.STATISTICS 
   WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME IN ('resource_skill', 'resource_subskill', 'delivery_role_expectation')
     AND INDEX_NAME LIKE 'idx_%';
   ```

## Rollback Procedure

If issues arise after deployment:

### PostgreSQL Rollback
```bash
psql -h localhost -U your_user -d your_db -f R1__Remove_Performance_Indexes.sql
```

### MySQL Rollback
```sql
-- Manually drop indexes
DROP INDEX IF EXISTS idx_resource_skill_proficiency_id ON resource_skill;
DROP INDEX IF EXISTS idx_resource_subskill_proficiency_id ON resource_subskill;
DROP INDEX IF EXISTS idx_delivery_role_expectation_skill_id ON delivery_role_expectation;
DROP INDEX IF EXISTS idx_delivery_role_expectation_sub_skill_id ON delivery_role_expectation;
DROP INDEX IF EXISTS idx_resource_skill_resource_id_active_flag ON resource_skill;
DROP INDEX IF EXISTS idx_resource_subskill_resource_id_active_flag ON resource_subskill;
DROP INDEX IF EXISTS idx_delivery_role_expectation_role_name_status ON delivery_role_expectation;
```

## Safety Features

### Idempotent Design
- All scripts check for existing indexes before creation
- Safe to run multiple times without side effects
- Rollback scripts verify index existence before dropping

### Production Safety
- No table locks during index creation (uses CREATE INDEX CONCURRENTLY in PostgreSQL)
- Minimal impact on ongoing operations
- Comprehensive error handling and logging

## Support

For issues or questions regarding this migration:
1. Check the database logs for error messages
2. Verify index creation using the verification queries
3. Monitor application performance metrics
4. Contact the database team if rollback is needed

---
**Migration Version**: 1.0  
**Created**: 2026-02-23  
**Compatible**: PostgreSQL 12+, MySQL 8.0+
