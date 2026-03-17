# Availability Recalculation Test Script

## Prerequisites
- Running RMS application
- Test resource with active allocation
- Database access for verification

## Test Steps

### 1. Find Active Allocation
```sql
SELECT allocation_id, resource_id, allocation_status, allocation_start_date, allocation_end_date
FROM resource_allocation 
WHERE allocation_status = 'ACTIVE' 
LIMIT 1;
```

### 2. Check Current Availability
```bash
curl -X GET "http://localhost:8080/api/availability/resource/{resourceId}?startDate=2026-03-01&endDate=2026-05-31"
```

### 3. Check Ledger Before De-allocation
```sql
SELECT resource_id, period_start, available_percentage, availability_trust_flag, last_calculated_at
FROM resource_availability_ledger 
WHERE resource_id = {resourceId}
AND period_start >= '2026-03-01'
ORDER BY period_start;
```

### 4. Perform De-allocation
```bash
curl -X POST "http://localhost:8080/api/allocations/{allocationId}/close" \
  -H "Content-Type: application/json" \
  -d '{
    "closureDate": "2026-03-17"
  }'
```

### 5. Verify Immediate Response
- Should return: `"Allocation closed successfully with immediate availability update"`
- Response time should be fast (no long delays)

### 6. Check Availability After De-allocation
```bash
curl -X GET "http://localhost:8080/api/availability/resource/{resourceId}?startDate=2026-03-01&endDate=2026-05-31"
```

### 7. Check Ledger After De-allocation
```sql
SELECT resource_id, period_start, available_percentage, availability_trust_flag, last_calculated_at
FROM resource_availability_ledger 
WHERE resource_id = {resourceId}
AND period_start >= '2026-03-01'
ORDER BY period_start;
```

## Expected Results

### ✅ Success Indicators
1. **Immediate API Response**: No delays in allocation closure response
2. **Updated Availability**: `available_percentage` should increase (more availability)
3. **Updated Trust Flag**: `availability_trust_flag` should be `true`
4. **Recent Calculation**: `last_calculated_at` should be very recent
5. **Multiple Periods Updated**: Current month + next 2 months should be recalculated
6. **Log Messages**: Should see async ledger update logs

### ❌ Failure Indicators
1. **Delayed Response**: API takes too long to respond
2. **No Availability Change**: `available_percentage` remains the same
3. **Old Timestamp**: `last_calculated_at` is not recent
4. **Error Messages**: Application logs show errors
5. **Transaction Rollback**: Allocation status doesn't change to ENDED

## Automated Verification

### Run Unit Tests
```bash
mvn test -Dtest=AllocationServiceImpleTest
```

### Check Application Logs
```bash
# Look for these messages:
# "Starting async ledger update for resource: {resourceId}"
# "Completed async ledger update for resource: {resourceId}"
# "Allocation closed successfully with immediate availability update"
```

## Performance Metrics
- **API Response Time**: < 2 seconds
- **Database Update Time**: < 5 seconds for all periods
- **Async Processing**: Should complete within 30 seconds
