# EOS → RMS CDC Deployment Checklist

## Pre-Deployment Requirements

### ✅ Database Setup
- [ ] EOS database is accessible with proper credentials
- [ ] Binary logging is enabled on EOS database
- [ ] Row-based binary logging format is configured
- [ ] Full row images are enabled in binary log
- [ ] EOS database user has REPLICATION SLAVE privilege
- [ ] EOS database user has SELECT privilege on target tables

### ✅ Application Configuration
- [ ] EOS CDC properties are configured in `application.properties`
- [ ] EOS database connection details are correct
- [ ] EOS table include list matches actual table names
- [ ] EOS CDC base directory exists and is writable
- [ ] EOS offset and schema history directories are accessible

### ✅ Infrastructure Setup
- [ ] Sufficient disk space for EOS offset and schema history files
- [ ] Network connectivity to EOS database from all RMS instances
- [ ] Firewall rules allow EOS database connections
- [ ] SSL/TLS certificates are configured if required
- [ ] Monitoring and alerting systems are configured

## Deployment Steps

### Phase 1: Configuration Validation
```bash
# 1. Validate EOS CDC configuration
curl -X GET "http://localhost:8080/actuator/health" | jq

# 2. Check EOS database connectivity
mysql -h $EOS_DB_HOST -P $EOS_DB_PORT -u $EOS_DB_USER -p$EOS_DB_PASSWORD -e "SHOW MASTER STATUS"

# 3. Verify binary logging configuration
mysql -h $EOS_DB_HOST -P $EOS_DB_PORT -u $EOS_DB_USER -p$EOS_DB_PASSWORD -e "SHOW VARIABLES LIKE 'binlog_format'"
mysql -h $EOS_DB_HOST -P $EOS_DB_PORT -u $EOS_DB_USER -p$EOS_DB_PASSWORD -e "SHOW VARIABLES LIKE 'binlog_row_image'"
```

### Phase 2: Application Deployment
```bash
# 1. Build application with EOS CDC components
./mvnw clean package -DskipTests

# 2. Deploy to first instance
scp target/ResourceManagementSystem.jar user@instance1:/opt/rms/
ssh user@instance1 "systemctl restart rms"

# 3. Verify EOS CDC startup
ssh user@instance1 "journalctl -u rms -f | grep 'EOS Debezium Engine'"
```

### Phase 3: Multi-Instance Deployment
```bash
# 1. Deploy to additional instances
for instance in instance2 instance3; do
    scp target/ResourceManagementSystem.jar user@$instance:/opt/rms/
    ssh user@$instance "systemctl restart rms"
done

# 2. Verify all instances are running
for instance in instance1 instance2 instance3; do
    echo "Checking $instance..."
    ssh user@$instance "systemctl status rms | grep Active"
    ssh user@$instance "ps aux | grep eos-debezium"
done
```

### Phase 4: Validation and Testing
```bash
# 1. Test EOS CDC with sample data
mysql -h $EOS_DB_HOST -P $EOS_DB_PORT -u $EOS_DB_USER -p$EOS_DB_PASSWORD -e "
INSERT INTO eos_entities (id, name, status, created_at) 
VALUES (1, 'Test Entity', 'ACTIVE', NOW());"

# 2. Verify CDC processing
curl -X GET "http://localhost:8080/actuator/metrics" | jq '.metrics[] | select(.name | contains("eos"))'

# 3. Check failure queue
curl -X GET "http://localhost:8080/api/cdc/failures?entityType=EOS_ENTITY"
```

## Post-Deployment Validation

### ✅ Health Checks
```bash
# Check application health
curl -X GET "http://localhost:8080/actuator/health" | jq

# Check EOS CDC specific health
curl -X GET "http://localhost:8080/actuator/health/eosCdc" | jq

# Verify Debezium engine status
curl -X GET "http://localhost:8080/actuator/info" | jq '.cdc.eos'
```

### ✅ Monitoring Setup
```bash
# Check EOS CDC metrics
curl -X GET "http://localhost:8080/actuator/metrics/eos.cdc.events.processed" | jq
curl -X GET "http://localhost:8080/actuator/metrics/eos.cdc.events.failed" | jq
curl -X GET "http://localhost:8080/actuator/metrics/eos.cdc.processing.latency" | jq

# Verify log output
tail -f /var/log/rms/application.log | grep EOS
```

### ✅ Multi-Instance Safety Validation
```bash
# Test concurrent processing
for i in {1..10}; do
    mysql -h $EOS_DB_HOST -P $EOS_DB_PORT -u $EOS_DB_USER -p$EOS_DB_PASSWORD -e "
    INSERT INTO eos_entities (id, name, status, created_at) 
    VALUES ($i, 'Test Entity $i', 'ACTIVE', NOW());"
    sleep 0.1
done

# Verify no duplicate processing
curl -X GET "http://localhost:8080/api/eos/entities" | jq '.[] | .id'
```

## Troubleshooting Guide

### Common Issues and Solutions

#### Issue: EOS Debezium Engine Fails to Start
**Symptoms**: Application logs show "EOS Debezium Engine failed to start"
**Solutions**:
```bash
# Check configuration
grep -i eos /opt/rms/application.properties

# Verify database connectivity
mysql -h $EOS_DB_HOST -P $EOS_DB_PORT -u $EOS_DB_USER -p$EOS_DB_PASSWORD -e "SELECT 1"

# Check file permissions
ls -la ~/eos-cdc/
```

#### Issue: Binary Logging Not Enabled
**Symptoms**: "Binary logging is not enabled" error
**Solutions**:
```sql
-- Check binary logging status
SHOW VARIABLES LIKE 'log_bin';
SHOW VARIABLES LIKE 'binlog_format';

-- Enable binary logging (requires MySQL restart)
SET GLOBAL log_bin = ON;
SET GLOBAL binlog_format = 'ROW';
```

#### Issue: Offset File Corruption
**Symptoms**: "Offset file is corrupted" error
**Solutions**:
```bash
# Backup and recreate offset file
mv ~/eos-cdc/eos-offsets.dat ~/eos-cdc/eos-offsets.dat.backup
systemctl restart rms

# Monitor for successful recreation
tail -f /var/log/rms/application.log | grep "EOS Debezium Engine started"
```

#### Issue: Schema History Conflicts
**Symptoms**: "Schema history conflicts" error
**Solutions**:
```bash
# Clear schema history (will trigger full snapshot)
rm ~/eos-cdc/eos-schema-history.dat
systemctl restart rms

# Monitor snapshot completion
tail -f /var/log/rms/application.log | grep "Snapshot completed"
```

## Performance Optimization

### Database Optimization
```sql
-- Optimize EOS database for CDC
SET GLOBAL binlog_cache_size = 1048576;
SET GLOBAL binlog_stmt_cache_size = 32768;
SET GLOBAL sync_binlog = 1;

-- Monitor binary log performance
SHOW VARIABLES LIKE 'binlog%';
SHOW STATUS LIKE 'Binlog%';
```

### Application Optimization
```properties
# EOS CDC performance tuning
eos.cdc.snapshot.fetch.size=1000
eos.cdc.max.batch.size=2048
eos.cdc.max.queue.size=8192
eos.cdc.poll.interval.ms=1000
```

### JVM Optimization
```bash
# Recommended JVM settings for EOS CDC
-Xms2g -Xmx4g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UseStringDeduplication
-Ddebezium.snapshot.fetch.size=1000
```

## Monitoring and Alerting

### Key Metrics to Monitor
1. **EOS Events Processed**: `eos.cdc.events.processed`
2. **EOS Events Failed**: `eos.cdc.events.failed`
3. **EOS Processing Latency**: `eos.cdc.processing.latency`
4. **EOS Queue Size**: `eos.cdc.queue.size`
5. **EOS Database Connection Pool**: `eos.cdc.db.pool.active`

### Alert Thresholds
```yaml
alerts:
  eos_cdc_events_failed:
    threshold: 5
    duration: 5m
    severity: warning
    
  eos_cdc_processing_latency:
    threshold: 30000ms
    duration: 10m
    severity: critical
    
  eos_cdc_queue_size:
    threshold: 1000
    duration: 5m
    severity: warning
```

### Dashboard Components
1. **EOS CDC Overview**: Overall health and status
2. **Event Processing**: Real-time event processing metrics
3. **Error Analysis**: Failed events and error patterns
4. **Performance**: Latency and throughput metrics
5. **Database Health**: Connection pool and query performance

## Rollback Plan

### Immediate Rollback
```bash
# 1. Stop EOS CDC processing
curl -X POST "http://localhost:8080/actuator/shutdown"

# 2. Revert application version
git checkout previous-version-tag
./mvnw clean package -DskipTests

# 3. Deploy previous version
scp target/ResourceManagementSystem.jar user@instance:/opt/rms/
ssh user@instance "systemctl restart rms"
```

### Data Consistency Check
```bash
# Verify data consistency after rollback
mysql -h $EOS_DB_HOST -P $EOS_DB_PORT -u $EOS_DB_USER -p$EOS_DB_PASSWORD -e "
SELECT COUNT(*) FROM eos_entities;"

curl -X GET "http://localhost:8080/api/eos/entities/count"
```

## Security Considerations

### Database Security
```sql
-- Create dedicated EOS CDC user
CREATE USER 'eos_cdc_user'@'%' IDENTIFIED BY 'strong_password';
GRANT REPLICATION SLAVE ON *.* TO 'eos_cdc_user'@'%';
GRANT SELECT ON eos_database.* TO 'eos_cdc_user'@'%';
FLUSH PRIVILEGES;
```

### Network Security
```bash
# Configure firewall for EOS database access
ufw allow from rms_instance_ip to any port 3306
ufw deny 3306
```

### Credential Management
```properties
# Use environment variables for sensitive data
eos.cdc.database.password=${EOS_DB_PASSWORD}
eos.cdc.database.user=${EOS_DB_USER}
```

## Maintenance Procedures

### Regular Maintenance
```bash
# Weekly maintenance tasks
0 2 * * 0 /opt/rms/scripts/eos-cdc-cleanup.sh
0 3 * * 0 /opt/rms/scripts/eos-cdc-health-check.sh
0 4 * * 0 /opt/rms/scripts/eos-cdc-metrics-collection.sh
```

### Backup Procedures
```bash
# Backup EOS CDC configuration and state
tar -czf /backup/eos-cdc-$(date +%Y%m%d).tar.gz ~/eos-cdc/
mysqldump -h $EOS_DB_HOST -u $EOS_DB_USER -p$EOS_DB_PASSWORD eos_database > /backup/eos-db-$(date +%Y%m%d).sql
```

## Documentation Updates

### Post-Deployment Documentation
- [ ] Update architecture diagrams
- [ ] Update runbooks and procedures
- [ ] Update monitoring dashboards
- [ ] Update team training materials
- [ ] Update incident response procedures

### Knowledge Transfer
- [ ] Conduct team training on EOS CDC
- [ ] Document lessons learned
- [ ] Share best practices
- [ ] Update on-call documentation
- [ ] Create troubleshooting guides

---

## Deployment Sign-off

**Lead Engineer**: _________________________ Date: _________

**QA Engineer**: _________________________ Date: _________

**DevOps Engineer**: _________________________ Date: _________

**Product Owner**: _________________________ Date: _________
