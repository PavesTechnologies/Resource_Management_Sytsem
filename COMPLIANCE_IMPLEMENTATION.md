# Compliance Requirements Mapping to Projects - Implementation Documentation

## Overview
This implementation provides end-to-end compliance management for projects in the Resource Management System. It allows Project Managers to map client compliance requirements to projects and enforces compliance during resource allocation.

## Features Implemented

### Task 1 - Select Applicable Client Compliance Rules
- **Endpoint**: `GET /api/v1/projects/{projectId}/compliance/available`
- **Functionality**: 
  - Retrieves all active client compliance requirements for a project's client
  - Excludes already mapped compliances
  - Clearly distinguishes between mandatory and optional compliances
  - Flags missing mandatory compliances before demand creation

### Task 2 - Control Mandatory vs Waived Compliance
- **Endpoints**:
  - `PUT /api/v1/projects/{projectId}/compliance/status` - Update compliance status
  - `GET /api/v1/projects/{projectId}/compliance/mandatory` - Get mandatory compliances
  - `GET /api/v1/projects/{projectId}/compliance/waived` - Get waived compliances
- **Functionality**:
  - Mark compliances as MANDATORY, WAIVED, or OPTIONAL
  - Waived compliances require justification
  - Full audit trail for all waiver actions
  - Authorization control for mandatory compliance waivers

### Task 3 - Enforce Compliance During Allocation
- **Integration Point**: `ProjectDemandValidationService.validateProjectForStaffing()`
- **Functionality**:
  - Blocks demand creation for non-compliant projects
  - Provides clear error messages for missing compliances
  - Validates resource compliance against project requirements
  - Real-time compliance status during allocation decisions

## Data Model

### Entities
1. **ProjectCompliance** - Maps client compliance to projects
   - Compliance status (MANDATORY, WAIVED, OPTIONAL)
   - Waiver justification and audit information
   - Audit timestamps

2. **ComplianceWaiverAudit** - Tracks all waiver actions
   - Before/after status tracking
   - User who performed the action
   - Timestamp and justification

3. **ComplianceStatus** - Enum for compliance states
   - MANDATORY - Required compliance
   - WAIVED - Compliance waived with justification
   - OPTIONAL - Optional compliance

### DTOs
- **ProjectComplianceDTO** - Transfer object for compliance data
- **ComplianceWaiverRequest** - Request for waiver actions
- **ProjectComplianceMappingRequest** - Request for mapping compliances
- **ComplianceValidationResult** - Validation response with details

## API Endpoints

### Compliance Management
```
GET    /api/v1/projects/{projectId}/compliance/available     # Get available compliances
GET    /api/v1/projects/{projectId}/compliance               # Get project compliances
POST   /api/v1/projects/{projectId}/compliance/map           # Map compliances to project
PUT    /api/v1/projects/{projectId}/compliance/status        # Update compliance status
```

### Validation & Monitoring
```
GET    /api/v1/projects/{projectId}/compliance/validate      # Validate project compliance
GET    /api/v1/projects/{projectId}/compliance/mandatory     # Get mandatory compliances
GET    /api/v1/projects/{projectId}/compliance/waived        # Get waived compliances
GET    /api/v1/projects/{projectId}/compliance/check-compliance # Check compliance status
POST   /api/v1/projects/{projectId}/compliance/flag-missing   # Flag missing compliances
GET    /api/v1/projects/{projectId}/compliance/resource/{resourceId}/validate # Validate resource compliance
```

## Security & Authorization
- **PROJECT_MANAGER**: Full access to map, update, and waive compliances
- **RESOURCE_MANAGER**: Read access to compliance status
- **DELIVERY_OWNER**: Read access and waiver permissions

## Error Handling
Custom exceptions with specific error codes:
- `PROJECT_NOT_FOUND` - Project does not exist
- `CLIENT_COMPLIANCE_NOT_FOUND` - Client compliance not found
- `COMPLIANCE_ALREADY_MAPPED` - Compliance already mapped to project
- `WAIVER_JUSTIFICATION_REQUIRED` - Justification required for waiver
- `MANDATORY_COMPLIANCE_WAIVER_NOT_ALLOWED` - Cannot waive mandatory compliance
- `COMPLIANCE_REQUIREMENTS_NOT_MET` - Project has missing mandatory compliances

## Integration Points

### Demand Creation Validation
The compliance validation is integrated into the demand creation process through `ProjectDemandValidationService`. When creating a demand:

1. Project status validation
2. Lifecycle stage validation  
3. **Compliance requirements validation** ← NEW
4. Governance validation

If compliance validation fails, demand creation is blocked with a clear error message.

### Resource Allocation
Compliance validation can be called during resource allocation to ensure resources meet project compliance requirements.

## Audit Trail
All waiver actions are tracked in `ComplianceWaiverAudit`:
- User who performed the action
- Timestamp of the action
- Previous and new status
- Justification for the action
- Type of action (GRANTED/REVOKED)

## Testing
Comprehensive unit tests cover:
- Compliance validation scenarios
- Waiver request processing
- Project compliance mapping
- Error handling and edge cases

## Database Schema Changes

### ProjectCompliance Table
```sql
ALTER TABLE project_compliance ADD COLUMN compliance_status VARCHAR(20) NOT NULL;
ALTER TABLE project_compliance ADD COLUMN waiver_justification VARCHAR(500);
ALTER TABLE project_compliance ADD COLUMN waived_by BIGINT;
ALTER TABLE project_compliance ADD COLUMN waived_at TIMESTAMP;
ALTER TABLE project_compliance ADD COLUMN mandatory_flag BOOLEAN NOT NULL;
ALTER TABLE project_compliance ADD COLUMN created_at TIMESTAMP;
ALTER TABLE project_compliance ADD COLUMN updated_at TIMESTAMP;
```

### ComplianceWaiverAudit Table
```sql
CREATE TABLE compliance_waiver_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_compliance_id UUID NOT NULL,
    waived_by BIGINT NOT NULL,
    waiver_justification VARCHAR(500) NOT NULL,
    waiver_action VARCHAR(20) NOT NULL,
    previous_status VARCHAR(20),
    new_status VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_compliance_id) REFERENCES project_compliance(id)
);
```

## Usage Examples

### 1. Map Compliances to Project
```bash
POST /api/v1/projects/123/compliance/map
{
  "projectId": 123,
  "selectedComplianceIds": ["uuid1", "uuid2"],
  "projectManagerId": 456
}
```

### 2. Waive a Compliance
```bash
PUT /api/v1/projects/123/compliance/status
{
  "projectComplianceId": "uuid1",
  "complianceStatus": "WAIVED",
  "waiverJustification": "Business requirement override",
  "waivedBy": 456
}
```

### 3. Validate Project Compliance
```bash
GET /api/v1/projects/123/compliance/validate

Response:
{
  "success": true,
  "data": {
    "compliant": false,
    "missingCompliances": ["Security Clearance", "Background Check"],
    "waivedCompliances": ["Tool Access"],
    "errorMessage": null
  }
}
```

## Benefits
1. **Compliance Enforcement**: Ensures all mandatory compliances are met before resource allocation
2. **Audit Trail**: Complete tracking of all waiver decisions and justifications
3. **Flexibility**: Supports mandatory, optional, and waived compliance states
4. **Integration**: Seamlessly integrates with existing demand creation workflow
5. **Visibility**: Clear compliance status for all stakeholders
6. **Error Prevention**: Proactive blocking of non-compliant allocations

This implementation provides a robust, auditable, and user-friendly compliance management system that integrates seamlessly with the existing Resource Management System workflow.
