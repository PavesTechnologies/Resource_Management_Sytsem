# Resource Removal from Organization Endpoint

## Overview
This endpoint allows you to remove a resource from the organization with proper notice period handling and optional immediate attrition trigger.

## Endpoint Details
- **URL**: `POST /allocation/role-off/resource-removal`
- **Description**: Removes a resource from the organization by marking them as ON_NOTICE and optionally triggering attrition
- **Authentication**: Required (CurrentUser)

## Request Body

```json
{
  "resourceId": 123,
  "noticePeriodEndDate": "2026-05-15",
  "removalReason": "Resignation - Better opportunity",
  "additionalComments": "Employee has accepted offer from competitor",
  "triggerAttritionImmediately": false
}
```

### Field Descriptions

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `resourceId` | Long | Yes | ID of the resource to remove |
| `noticePeriodEndDate` | LocalDate | Yes | Last working day of the resource (must be today or future) |
| `removalReason` | String | Yes | Reason for resource removal |
| `additionalComments` | String | No | Additional context or comments |
| `triggerAttritionImmediately` | Boolean | No (default: false) | Whether to trigger attrition flow immediately |

## Response

```json
{
  "message": "Resource removal processed successfully. Resource marked as ON_NOTICE with notice period until 2026-05-15. Attrition will be triggered automatically.",
  "resourceId": 123,
  "noticePeriodEndDate": "2026-05-15",
  "triggerAttritionImmediately": false
}
```

## Processing Flow

### 1. Resource Validation
- Validates that the resource exists
- Checks that the resource is not already in EXITED status
- Validates notice period end date is not in the past

### 2. Resource Status Update
- Sets `employmentStatus` to `ON_NOTICE`
- Sets `dateOfExit` to the provided notice period end date
- Sets `noticeStartDate` to current date
- Sets `noticeEndDate` to the provided notice period end date
- Updates audit fields (`changedBy`, `changedAt`, `statusEffectiveFrom`)

### 3. Attrition Trigger (Optional)
- If `triggerAttritionImmediately` is `true`:
  - Calls `handleAttrition()` method immediately
  - Closes all active allocations
  - Creates replacement demands
  - Updates availability ledger
- If `triggerAttritionImmediately` is `false`:
  - Attrition will be triggered automatically by `ResourceService` when it detects ON_NOTICE status with dateOfExit

## Attrition Process Details

When attrition is triggered (immediately or automatically), the following happens:

1. **Allocation Processing**:
   - Active allocations overlapping with exit date are closed
   - Future allocations are cancelled
   - Replacement demands are created for affected allocations

2. **Availability Ledger Updates**:
   - Resource availability is recalculated from exit date
   - Ledger entries are updated for 90-day horizon
   - Cross-module synchronization is triggered

3. **Audit Trail**:
   - All actions are logged with user details
   - Resource status changes are tracked
   - Allocation closures are recorded

## Usage Examples

### Example 1: Standard Notice Period (Auto Attrition)
```bash
curl -X POST http://localhost:8080/allocation/role-off/resource-removal \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "resourceId": 123,
    "noticePeriodEndDate": "2026-05-15",
    "removalReason": "Resignation - Personal reasons",
    "additionalComments": "Serving 30-day notice period",
    "triggerAttritionImmediately": false
  }'
```

### Example 2: Immediate Attrition Trigger
```bash
curl -X POST http://localhost:8080/allocation/role-off/resource-removal \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "resourceId": 456,
    "noticePeriodEndDate": "2026-04-10",
    "removalReason": "Performance issues",
    "additionalComments": "Immediate termination required",
    "triggerAttritionImmediately": true
  }'
```

## Error Responses

### Resource Not Found (404)
```json
{
  "status": 404,
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "Resource not found with ID: 999"
}
```

### Resource Already Exited (400)
```json
{
  "status": 400,
  "errorCode": "RESOURCE_ALREADY_EXITED",
  "message": "Resource has already exited the organization"
}
```

### Invalid Notice Period Date (400)
```json
{
  "status": 400,
  "message": "Notice period end date must be today or in the future"
}
```

## Integration with Existing System

This endpoint integrates with:

1. **RoleOffService**: Uses existing attrition handling logic
2. **ResourceService**: Automatic attrition trigger when ON_NOTICE is detected
3. **AllocationService**: Allocation closure and replacement demand creation
4. **AvailabilityLedgerAsyncService**: Resource availability updates
5. **DemandService**: Replacement demand creation

## Best Practices

1. **Use Immediate Trigger**: Set `triggerAttritionImmediately: true` when you need immediate resource replacement planning
2. **Use Auto Trigger**: Set `triggerAttritionImmediately: false` when following standard notice period procedures
3. **Provide Clear Reasons**: Include detailed removal reasons for audit and reporting purposes
4. **Monitor Attrition**: Check system logs and role-off events after processing

## Notes

- The endpoint validates that the notice period end date is not in the past
- Resources already in EXITED status cannot be processed again
- All actions are logged with the processing user's details
- The method is transactional - any failure rolls back all changes
