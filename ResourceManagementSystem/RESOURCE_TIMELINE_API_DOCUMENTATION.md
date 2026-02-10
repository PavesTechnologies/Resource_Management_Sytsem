# Resource Timeline API - Empty Data Validation Documentation

## Overview

The Resource Timeline API is designed to be **fully dynamic** and work correctly with zero or many records using only aggregation-based calculations. No conditional logic based on data existence is used anywhere in the implementation.

## REST API Contract

### Endpoint
```
GET /api/availability/timeline
```

### Response Format
```json
[
  {
    "id": "res-001",
    "name": "Aarav Sharma",
    "avatar": "AS",
    "role": "Technical Lead",
    "skills": ["Java", "Spring"],
    "location": "Bangalore",
    "experience": 12,
    "currentAllocation": 100,
    "availableFrom": "2026-04-17",
    "currentProject": "Project Atlas",
    "nextAssignment": "Project Keystone",
    "employmentType": "Billable",
    "utilizationHistory": [92, 88, 95, 100, 97, 100, 85, 90, 95, 100, 100, 98],
    "allocationTimeline": [
      {
        "project": "Project Genesis",
        "startDate": "2025-07-01",
        "endDate": "2025-10-01",
        "allocation": 100,
        "tentative": false
      }
    ]
  }
]
```

## Empty Data Validation Scenarios

### Scenario 1: RESOURCE_ALLOCATION has 0 rows, RESOURCE_AVAILABILITY_LEDGER has data

**Expected Behavior:**
- `currentAllocation`: 0 (COALESCE handles null allocation)
- `utilizationHistory`: Populated from ledger data
- `allocationTimeline`: Empty list (LEFT JOIN returns no rows)
- `currentProject`: null (no ACTIVE allocations found)
- `nextAssignment`: null (no future allocations found)

**SQL Logic:**
```sql
-- currentAllocation calculation
COALESCE(latest_ledger.confirmed_alloc_hours * 100.0 / NULLIF(latest_ledger.standard_hours, 0), 0)

-- allocationTimeline (empty because LEFT JOIN on resource_allocation returns no rows)
LEFT JOIN resource_allocation ra ON r.resource_id = ra.resource_id
WHERE ra.allocation_status IN ('ACTIVE', 'PLANNED')  -- No rows match
```

### Scenario 2: RESOURCE_AVAILABILITY_LEDGER has 0 rows, RESOURCE_ALLOCATION has data

**Expected Behavior:**
- `currentAllocation`: 0 (COALESCE handles null ledger)
- `utilizationHistory`: Empty list (no ledger rows for last 12 months)
- `allocationTimeline`: Populated from allocation data
- `availableFrom`: null (no future availability periods found)

**SQL Logic:**
```sql
-- currentAllocation calculation
COALESCE(latest_ledger.confirmed_alloc_hours * 100.0 / NULLIF(latest_ledger.standard_hours, 0), 0)
-- latest_ledger is null due to LEFT JOIN on empty ledger table

-- utilizationHistory (empty because no ledger rows exist)
SELECT ROUND(COALESCE(confirmed_alloc_hours * 100.0 / NULLIF(standard_hours, 0), 0)) as utilization
FROM resource_availability_ledger
WHERE resource_id = :resourceId  -- No rows found
```

### Scenario 3: Both RESOURCE_ALLOCATION and RESOURCE_AVAILABILITY_LEDGER have 0 rows

**Expected Behavior:**
- `currentAllocation`: 0 (COALESCE handles null ledger)
- `utilizationHistory`: Empty list (no ledger data)
- `allocationTimeline`: Empty list (no allocation data)
- `currentProject`: null (no allocations found)
- `nextAssignment`: null (no future allocations found)
- `availableFrom`: null (no availability periods found)

**SQL Logic:**
```sql
-- All LEFT JOINs return null, all COALESCE provide defaults
COALESCE(latest_ledger.confirmed_alloc_hours * 100.0 / NULLIF(latest_ledger.standard_hours, 0), 0) = 0
COALESCE(current_proj.project_name, null) = null
COALESCE(next_assign.project_name, null) = null
```

### Scenario 4: All tables have data

**Expected Behavior:**
- All fields populated with actual calculated values
- Aggregations work normally with real data

## Key Aggregation-Only Techniques

### 1. COALESCE for Default Values
```sql
COALESCE(field, default_value)
```
- Eliminates null checks
- Provides sensible defaults when data doesn't exist

### 2. NULLIF for Division Safety
```sql
NULLIF(denominator, 0)
```
- Prevents division by zero
- Returns null when denominator is 0, handled by COALESCE

### 3. LEFT JOIN for Optional Data
```sql
FROM resource r
LEFT JOIN resource_allocation ra ON r.resource_id = ra.resource_id
```
- Ensures main query returns resources even when related data is missing
- No conditional logic needed for empty related tables

### 4. Subqueries with LIMIT 1 for Single Values
```sql
LEFT JOIN (
    SELECT resource_id, confirmed_alloc_hours, standard_hours
    FROM resource_availability_ledger 
    WHERE period_start <= CURRENT_DATE AND period_end >= CURRENT_DATE
    ORDER BY period_start DESC LIMIT 1
) latest_ledger ON r.resource_id = latest_ledger.resource_id
```
- Returns single row or null naturally
- No existence checks needed

### 5. Aggregate Functions for Collections
```sql
-- Empty collections naturally return empty lists
SELECT allocation_timeline FROM resource_allocation WHERE resource_id = x

-- Utilization history naturally empty when no ledger rows
SELECT utilization FROM resource_availability_ledger WHERE resource_id = x
```

## Field-by-Field Empty Data Behavior

| Field | Empty Data Behavior | Calculation Method |
|-------|-------------------|-------------------|
| `id` | Always populated | Direct from RESOURCE.resource_id |
| `name` | Always populated | Direct from RESOURCE.full_name |
| `avatar` | Generated from name | String manipulation on name |
| `role` | null if no designation | Direct from RESOURCE.designation |
| `skills` | Empty list | LEFT JOIN on skill tables |
| `location` | null if no location | Direct from RESOURCE.working_location |
| `experience` | 0 if no joining date | AGE calculation with COALESCE |
| `currentAllocation` | 0 if no ledger data | COALESCE on ledger calculation |
| `availableFrom` | null if no future availability | LEFT JOIN on future periods |
| `currentProject` | null if no active allocation | LEFT JOIN with date filter |
| `nextAssignment` | null if no future allocation | LEFT JOIN with future date filter |
| `employmentType` | Always populated | Direct from RESOURCE.employment_type |
| `utilizationHistory` | Empty list if no ledger | LEFT JOIN on last 12 months |
| `allocationTimeline` | Empty list if no allocations | LEFT JOIN on allocation table |

## Implementation Guarantees

1. **No Conditional Logic**: Zero `if` statements checking for data existence
2. **Aggregation Only**: All calculations use SUM, COUNT, COALESCE, LEFT JOIN
3. **Natural Empty Results**: Empty tables naturally return zeros, nulls, empty lists
4. **Single Code Path**: Same execution path regardless of data presence
5. **No Hardcoded Values**: All values derived from database calculations

## Testing Empty Data Scenarios

To validate empty data behavior:

1. **Truncate allocation table**:
   ```sql
   TRUNCATE TABLE resource_allocation;
   ```
   - API should return resources with 0% allocation, empty timeline

2. **Truncate ledger table**:
   ```sql
   TRUNCATE TABLE resource_availability_ledger;
   ```
   - API should return resources with 0% allocation, empty utilization history

3. **Truncate both tables**:
   ```sql
   TRUNCATE TABLE resource_allocation;
   TRUNCATE TABLE resource_availability_ledger;
   ```
   - API should return resources with only basic info, all calculated fields as defaults

The API will continue to function correctly in all scenarios without any code changes.
