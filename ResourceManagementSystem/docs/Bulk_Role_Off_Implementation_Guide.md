# Bulk Role-Off Implementation Guide

## Overview

This guide explains the bulk role-off functionality implemented for planned role types, specifically designed for scenarios like project endings where multiple resources need to be role-off simultaneously.

## Key Features

### 1. **Planned Role Type Only**
- Bulk role-off is restricted to PLANNED role type only
- Emergency role-offs must still be processed individually for proper governance
- Effective date cannot be in the past

### 2. **Impact Preview**
- Pre-execution impact analysis for all resources
- Utilization impact calculation
- High-impact resource identification
- Bulk confirmation requirement

### 3. **Bulk Approval Workflow**
- Resource Manager can bulk approve/reject pending role-offs
- Delivery Manager can bulk fulfill/reject approved role-offs
- Maintains existing approval hierarchy

## API Endpoints

### 1. Create Bulk Planned Role-Off
```
POST /api/role-off/bulk-planned
Authorization: PROJECT-MANAGER or RESOURCE-MANAGER
```

**Request Body:**
```json
{
  "projectId": 123,
  "allocationIds": ["uuid1", "uuid2", "uuid3"],
  "effectiveRoleOffDate": "2024-12-31",
  "roleOffReason": "PROJECT_END",
  "confirmed": false
}
```

**Response (Preview Mode):**
```json
{
  "requiresConfirmation": true,
  "warning": "Bulk Role-Off Impact Preview\n\nProject: Project Name\nTotal Resources: 3\nTotal Utilization Impact: 250%\nHigh Impact Resources: 1\n\n⚠️ WARNING: 1 resources have HIGH impact on project delivery.\nRecommendation: Consider staggering role-offs or ensure replacements are ready.\n\nDo you want to proceed with bulk role-off?",
  "message": "Please review bulk role-off impact and confirm to proceed",
  "resourceImpacts": [
    {
      "allocationId": "uuid1",
      "resourceId": 456,
      "resourceName": "John Doe",
      "currentUtilization": 100,
      "impactLevel": "HIGH",
      "allocationPercentage": 100
    }
  ],
  "projectId": 123,
  "totalResources": 3,
  "highImpactCount": 1
}
```

**Response (After Confirmation):**
```json
{
  "success": true,
  "message": "All bulk role-off requests created successfully",
  "data": {
    "successCount": 3,
    "failedCount": 0,
    "success": [...],
    "failed": []
  }
}
```

### 2. Bulk RM Approve
```
POST /api/role-off/bulk-rm-approve
Authorization: RESOURCE-MANAGER
```

**Request Body:**
```json
["uuid1", "uuid2", "uuid3"]
```

### 3. Bulk RM Reject
```
POST /api/role-off/bulk-rm-reject?rejectionReason=Project%20timeline%20extended
Authorization: RESOURCE-MANAGER
```

**Request Body:**
```json
["uuid1", "uuid2", "uuid3"]
```

### 4. Bulk DL Fulfill
```
POST /api/role-off/bulk-dl-fulfill
Authorization: DELIVERY-MANAGER
```

**Request Body:**
```json
["uuid1", "uuid2", "uuid3"]
```

### 5. Bulk DL Reject
```
POST /api/role-off/bulk-dl-reject?rejectionReason=Resources%20still%20needed
Authorization: DELIVERY-MANAGER
```

**Request Body:**
```json
["uuid1", "uuid2", "uuid3"]
```

## Validation Rules

### Bulk Role-Off Request Validation
- `projectId`: Required and must exist
- `allocationIds`: Required, non-empty list
- `effectiveRoleOffDate`: Required, must be today or future
- `roleOffReason`: Required enum value
- `confirmed`: Must be true for actual execution (false for preview)
- **Auto-replacement is not allowed for bulk role-offs**

### Allocation Validation
- All allocations must belong to the specified project
- All allocations must be in ACTIVE status
- No existing role-off events (except REJECTED ones)

### Approval Workflow Validation
- RM can only approve/reject PENDING events
- DL can only fulfill/reject RM-approved events
- Rejection reasons are mandatory for bulk rejections

## Business Logic

### 1. Impact Calculation
- Uses existing `calculateBatchImpactLevels` method
- Considers utilization, project criticality, skill criticality
- Identifies HIGH impact resources for warnings

### 2. Replacement Demand Creation
- **Auto-replacement is disabled for bulk role-offs**
- Manual replacement demands must be created individually if needed
- All bulk role-offs are marked with replacement status SKIPPED

### 3. Execution Flow
1. **Preview Mode**: Shows impact analysis, requires confirmation
2. **Creation**: Creates role-off events with PENDING status
3. **RM Approval**: Resource Manager bulk approval
4. **DL Fulfillment**: Delivery Manager bulk fulfillment
5. **Execution**: Automatic execution on effective date

## Error Handling

### Partial Success Handling
- Individual allocation failures don't break entire operation
- Detailed response with success/failure counts
- Specific failure reasons for each allocation

### Common Error Responses
```json
{
  "success": false,
  "message": "Invalid bulk role-off request. Please check all required fields.",
  "data": null
}
```

```json
{
  "success": false,
  "message": "Rejection reason is required for bulk rejection",
  "data": null
}
```

## Usage Examples

### Scenario 1: Project End Role-Off
```bash
# Step 1: Preview impact
curl -X POST http://localhost:8080/api/role-off/bulk-planned \
  -H "Authorization: Bearer PM_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "projectId": 123,
    "allocationIds": ["uuid1", "uuid2", "uuid3"],
    "effectiveRoleOffDate": "2024-12-31",
    "roleOffReason": "PROJECT_END",
    "confirmed": false
  }'

# Step 2: Execute after review
curl -X POST http://localhost:8080/api/role-off/bulk-planned \
  -H "Authorization: Bearer PM_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "projectId": 123,
    "allocationIds": ["uuid1", "uuid2", "uuid3"],
    "effectiveRoleOffDate": "2024-12-31",
    "roleOffReason": "PROJECT_END",
    "confirmed": true
  }'
```

### Scenario 2: RM Bulk Approval
```bash
curl -X POST http://localhost:8080/api/role-off/bulk-rm-approve \
  -H "Authorization: Bearer RM_TOKEN" \
  -H "Content-Type: application/json" \
  -d '["uuid1", "uuid2", "uuid3"]'
```

## Security Considerations

### Role-Based Access
- **PROJECT-MANAGER**: Can create bulk role-offs for their projects
- **RESOURCE-MANAGER**: Can create bulk role-offs and approve/reject
- **DELIVERY-MANAGER**: Can fulfill/reject approved role-offs
- **ADMIN**: Full access to all endpoints

### Data Validation
- All inputs validated at service and controller levels
- Project ownership verification
- Allocation status verification
- Audit logging for all operations

## Integration with Existing Flow

### Non-Breaking Changes
- All existing single role-off functionality remains unchanged
- Uses existing repositories and services
- Maintains existing approval workflow
- Compatible with existing scheduler for execution

### Reused Components
- `RoleOffEvent` entity (no changes needed)
- `AllocationRepository` and `RoleOffEventRepository`
- `createReplacementDemand` method
- `calculateBatchImpactLevels` method
- `closeResourceAllocation` method

## Testing Recommendations

### Unit Tests
- Test validation logic for bulk requests
- Test impact calculation accuracy
- Test partial success scenarios
- Test error handling

### Integration Tests
- Test complete workflow: creation → approval → fulfillment → execution
- Test with different user roles
- Test replacement demand creation
- Test scheduler integration

### Performance Tests
- Test with large number of allocations (50+)
- Test concurrent bulk operations
- Test database transaction boundaries

## Future Enhancements

### Potential Improvements
1. **Staggered Role-Offs**: Option to automatically schedule role-offs over time
2. **Skill-Based Grouping**: Group resources by skill for better replacement planning
3. **Notification System**: Automated notifications for stakeholders
4. **Reporting**: Bulk operation analytics and reporting
5. **Rollback**: Ability to rollback bulk operations

### Configuration Options
- Maximum number of allocations per bulk request
- Mandatory impact review threshold
- Auto-approval rules for low-impact role-offs

## Conclusion

The bulk role-off functionality provides a efficient way to handle planned role-offs for multiple resources while maintaining governance and control. The implementation ensures:

- **Safety**: Impact preview and confirmation requirements
- **Flexibility**: Partial success handling and detailed responses
- **Integration**: Seamless integration with existing workflows
- **Security**: Role-based access control and validation
- **Scalability**: Efficient batch processing for large operations

This solution addresses the specific need for project end scenarios while preserving the integrity of the existing role-off system.
