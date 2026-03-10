---
description: Resource Displacement Workflow for Priority-Based Allocation Management
---

# Resource Displacement Workflow

## Overview
This workflow allows Resource Managers to displace lower-priority soft allocations in favor of higher-priority client demands, ensuring critical work is protected.

## Prerequisites
- User must have RESOURCE-MANAGER or ADMIN role
- Resource must have existing allocations with conflicting priorities
- Higher-priority demand must be confirmed/active

## Workflow Steps

### 1. Detect Allocation Conflicts
**Endpoint**: `POST /api/allocation-conflicts/detect/{resourceId}`

**Example Request**:
```bash
POST /api/allocation-conflicts/detect/123
Authorization: Bearer {token}
```

**Response**: Returns list of detected conflicts with severity levels and recommendations.

### 2. Review Pending Conflicts
**Endpoint**: `GET /api/allocation-conflicts/pending`

**Example Response**:
```json
{
  "success": true,
  "data": [
    {
      "conflictId": "550e8400-e29b-41d4-a716-446655440000",
      "resourceId": 123,
      "resourceName": "John Doe",
      "conflictType": "PRIORITY_MISMATCH",
      "conflictSeverity": "HIGH",
      "lowerPriorityClientName": "Bronze Client",
      "higherPriorityClientName": "Platinum Client",
      "recommendation": "Consider upgrading the Platinum client's SOFT allocation or adjusting the Bronze client's CONFIRMED allocation.",
      "resolutionStatus": "PENDING"
    }
  ]
}
```

### 3. Resolve Conflict with Displacement
**Endpoint**: `POST /api/allocation-conflicts/{conflictId}/resolve`

**Example Request**:
```json
{
  "resolutionAction": "DISPLACE",
  "reason": "Platinum client has critical deadline",
  "resolvedBy": "resource.manager@company.com",
  "resolutionNotes": "Displacing Bronze client allocation to accommodate Platinum client priority demand"
}
```

### 4. System Actions Performed

#### For DISPLACE Action:
1. **Cancel Lower-Priority Allocation**: Sets allocation status to `CANCELLED`
2. **Preserve Higher-Priority Allocation**: Keeps the higher-priority allocation active
3. **Update Conflict Status**: Marks conflict as `RESOLVED`
4. **Record Resolution**: Stores who made the decision and why

#### Backend Processing:
```java
// From AllocationConflictService.java
private void displaceAllocation(UUID allocationId) {
    ResourceAllocation allocation = allocationRepository.findById(allocationId)
            .orElseThrow(() -> new RuntimeException("Allocation not found: " + allocationId));
    
    allocation.setAllocationStatus(AllocationStatus.CANCELLED);
    allocationRepository.save(allocation);
}
```

## Alternative Resolution Actions

### UPGRADE Action
- Upgrades higher-priority allocation from PLANNED → ACTIVE
- Converts demand commitment from SOFT → CONFIRMED
- Keeps lower-priority allocation but may adjust dates

### KEEP_CURRENT Action
- Maintains existing allocation status
- Rejects higher-priority request
- May require manual intervention or resource reallocation

## Conflict Severity Levels

- **HIGH**: Tier difference of 3+ levels (e.g., Bronze vs Platinum)
- **MEDIUM**: Tier difference of 2 levels (e.g., Bronze vs Gold)
- **LOW**: Tier difference of 1 level (e.g., Silver vs Gold)

## Business Rules

1. **Priority Hierarchy**: CRITICAL > HIGH > MEDIUM > LOW
2. **Client Tier Mapping**:
   - CRITICAL → TIER_1_PLATINUM
   - HIGH → TIER_2_GOLD
   - MEDIUM → TIER_3_SILVER
   - LOW → TIER_4_BRONZE

3. **Allocation Status Impact**:
   - ACTIVE allocations have higher priority than PLANNED
   - CONFIRMED demands take precedence over SOFT demands

## Error Handling

Common error scenarios:
- **Conflict not found**: Invalid conflictId
- **Allocation not found**: Referenced allocation doesn't exist
- **Insufficient permissions**: User lacks required role
- **Already resolved**: Conflict has been processed

## Monitoring & Auditing

All resolution actions are tracked with:
- Timestamp of resolution
- User who made the decision
- Reason and notes
- Before/after allocation states

## Integration Points

This workflow integrates with:
- **Allocation Service**: Manages resource allocations
- **Demand Service**: Validates demand priorities
- **Project Service**: Links allocations to projects
- **Resource Service**: Validates resource availability

## Limitations

- **No Automated Notifications**: PMs must be manually notified of displacements
- **No Soft Allocation Distinction**: System uses general priority levels
- **Manual Process**: Requires manager intervention for each conflict

## Future Enhancements

1. **Automated PM Notifications**: Email/alert system for displaced allocations
2. **Soft Allocation Logic**: Enhanced handling of soft vs hard commitments
3. **Bulk Resolution**: Process multiple conflicts simultaneously
4. **Conflict Prevention**: Proactive conflict avoidance during allocation creation
