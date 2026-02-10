# Workforce Availability Engine - Complete Example

## Sample Resource Data
```sql
-- Resource record
INSERT INTO resource (resource_id, employee_code, full_name, date_of_joining, date_of_exit, employment_status, active_flag)
VALUES (123, 'EMP001', 'John Doe', '2026-01-15', NULL, 'ACTIVE', true);
```

## Sample External API Data for February 2026

### Holiday API Response
```json
[
  {
    "holidayId": 45,
    "holidayName": "Republic Day",
    "holidayDate": "2026-01-26",
    "type": "COMPULSORY",
    "state": "All",
    "country": "India",
    "year": 2026,
    "isActive": true
  }
]
```

### Leave API Response
```json
{
  "success": true,
  "data": [
    {
      "employeeId": "EMP001",
      "employeeName": "John Doe",
      "approvedLeaveDates": ["2026-02-10", "2026-02-11", "2026-02-24"]
    }
  ]
}
```

## SQL Calculation Logic

### Step 1: Calculate Working Days in February 2026
```sql
-- February 2026 has 20 working days (excluding weekends)
-- Weekends: Feb 7-8, Feb 14-15, Feb 21-22, Feb 28

WITH working_days AS (
  SELECT generate_series(
    '2026-02-01'::date,
    '2026-02-28'::date,
    '1 day'::interval
  )::date as calendar_date
  WHERE EXTRACT(DOW FROM generate_series) NOT IN (0, 6) -- Exclude Sunday(0), Saturday(6)
),

effective_period AS (
  SELECT 
    CASE 
      WHEN '2026-01-15' > '2026-02-01' THEN '2026-01-15' -- After joining
      ELSE '2026-02-01'
    END as effective_start,
    '2026-02-28' as effective_end
),

effective_working_days AS (
  SELECT wd.calendar_date
  FROM working_days wd, effective_period ep
  WHERE wd.calendar_date >= ep.effective_start 
    AND wd.calendar_date <= ep.effective_end
)

SELECT COUNT(*) as working_days FROM effective_working_days;
-- Result: 20 working days (Feb 15-28 only, since joined Jan 15)
```

### Step 2: Calculate Holiday Hours
```sql
WITH holidays_2026 AS (
  SELECT '2026-01-26'::date as holiday_date -- Republic Day
  UNION ALL
  SELECT '2026-02-23'::date as holiday_date -- Sample holiday in Feb
),

working_days_feb AS (
  SELECT generate_series('2026-02-15', '2026-02-28', '1 day')::date as work_date
  WHERE EXTRACT(DOW FROM generate_series) NOT IN (0, 6)
),

feb_holidays AS (
  SELECT wd.work_date
  FROM working_days_feb wd
  JOIN holidays_2026 h ON wd.work_date = h.holiday_date
)

SELECT COUNT(*) as holiday_days FROM feb_holidays;
-- Result: 1 holiday day (Feb 23)
-- Holiday hours = 1 * 8 = 8 hours
```

### Step 3: Calculate Leave Hours
```sql
WITH leave_dates AS (
  SELECT '2026-02-10'::date as leave_date
  UNION ALL
  SELECT '2026-02-11'::date as leave_date
  UNION ALL
  SELECT '2026-02-24'::date as leave_date
),

working_days_feb AS (
  SELECT generate_series('2026-02-15', '2026-02-28', '1 day')::date as work_date
  WHERE EXTRACT(DOW FROM generate_series) NOT IN (0, 6)
),

holidays_feb AS (
  SELECT '2026-02-23'::date as holiday_date
),

effective_leave_days AS (
  SELECT ld.leave_date
  FROM leave_dates ld
  JOIN working_days_feb wd ON ld.leave_date = wd.work_date
  LEFT JOIN holidays_feb h ON ld.leave_date = h.holiday_date
  WHERE h.holiday_date IS NULL -- Exclude overlapping holidays
)

SELECT COUNT(*) as leave_days FROM effective_leave_days;
-- Result: 0 leave days (all leave dates are before joining date Feb 15)
-- Leave hours = 0 * 8 = 0 hours
```

### Step 4: Final Availability Calculation
```sql
-- Standard Hours = 20 working days * 8 hours = 160 hours
-- Holiday Hours = 1 holiday * 8 hours = 8 hours  
-- Leave Hours = 0 leave * 8 hours = 0 hours
-- Confirmed Alloc Hours = 0 (Phase 1)
-- Draft Alloc Hours = 0 (Phase 1)

-- Firm Available Hours = 160 - 8 - 0 - 0 = 152 hours
-- Projected Available Hours = 152 - 0 = 152 hours
```

## Final Ledger Record
```sql
INSERT INTO resource_availability_ledger (
    resource_id, period_start, period_end, standard_hours, holiday_hours, 
    leave_hours, confirmed_alloc_hours, draft_alloc_hours, firm_available_hours,
    projected_available_hours, availability_trust_flag, last_calculated_at
) VALUES (
    123,                           -- resource_id
    '2026-02-01',                  -- period_start (month start)
    '2026-02-28',                  -- period_end (month end)
    160,                           -- standard_hours (20 days * 8)
    8,                             -- holiday_hours (1 holiday * 8)
    0,                             -- leave_hours (0 effective leave days)
    0,                             -- confirmed_alloc_hours (Phase 1)
    0,                             -- draft_alloc_hours (Phase 1)
    152,                           -- firm_available_hours (160-8-0-0)
    152,                           -- projected_available_hours (152-0)
    true,                          -- availability_trust_flag (APIs healthy, resource active)
    '2026-02-09 18:25:00'         -- last_calculated_at
);
```

## Query Examples

### Get Available Hours for Resource in Month
```sql
SELECT 
    resource_id,
    period_start,
    period_end,
    firm_available_hours,
    projected_available_hours,
    availability_trust_flag
FROM resource_availability_ledger
WHERE resource_id = 123 
  AND period_start = '2026-02-01';
```

### Get Monthly Summary for All Resources
```sql
SELECT 
    r.full_name,
    r.employee_code,
    ral.period_start,
    ral.standard_hours,
    ral.firm_available_hours,
    ral.projected_available_hours,
    ral.availability_trust_flag
FROM resource_availability_ledger ral
JOIN resource r ON ral.resource_id = r.resource_id
WHERE ral.period_start = '2026-02-01'
ORDER BY ral.firm_available_hours DESC;
```

### Find Resources with Low Availability
```sql
SELECT 
    r.resource_id,
    r.full_name,
    ral.period_start,
    ral.firm_available_hours,
    (ral.firm_available_hours::decimal / ral.standard_hours * 100) as availability_percentage
FROM resource_availability_ledger ral
JOIN resource r ON ral.resource_id = r.resource_id
WHERE ral.period_start = '2026-02-01'
  AND ral.availability_trust_flag = true
  AND ral.firm_available_hours < 120 -- Less than 15 days available
ORDER BY availability_percentage ASC;
```
