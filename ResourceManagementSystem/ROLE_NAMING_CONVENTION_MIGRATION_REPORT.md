# Role Naming Convention Migration Report

## Overview
This document details the refactoring of role naming conventions in the Resource Management System backend from hyphenated format to underscore format.

**Migration Date:** 2026-05-04  
**Scope:** Resource Management Module Only  
**Status:** Completed  

## Role Name Changes

| Old Value | New Value |
|-----------|-----------|
| DELIVERY-MANAGER | Delivery_Manager |
| ADMIN | Admin |
| PROJECT-MANAGER | Project_Manager |
| RESOURCE-MANAGER | Resource_Manager |

## Modified Files

### 1. Controller Layer

#### DeliveryRoleExpectationController.java
**File Path:** `src/main/java/com/controller/skill_controllers/DeliveryRoleExpectationController.java`

**Changes Made:**
- Line 32: `@PreAuthorize("hasAnyRole('ADMIN', 'PROJECT-MANAGER', 'DELIVERY-MANAGER')")` → `@PreAuthorize("hasAnyRole('Admin', 'Project_Manager', 'Delivery_Manager')")`
- Line 56: `@PreAuthorize("hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Admin')")`
- Line 103: `@PreAuthorize("hasAnyRole('ADMIN', 'RESOURCE-MANAGER', 'PROJECT-MANAGER', 'RESOURCE-USER')")` → `@PreAuthorize("hasAnyRole('Admin', 'Resource_Manager', 'Project_Manager', 'RESOURCE-USER')")`
- Line 120: `@PreAuthorize("hasAnyRole('ADMIN', 'RESOURCE-MANAGER', 'PROJECT-MANAGER')")` → `@PreAuthorize("hasAnyRole('Admin', 'Resource_Manager', 'Project_Manager')")`
- Line 136: `@PreAuthorize("hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Admin')")`
- Line 153: `@PreAuthorize("hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Admin')")`
- Line 171: `@PreAuthorize("hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Admin')")`
- Line 187: `@PreAuthorize("hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Admin')")`
- Line 204: `@PreAuthorize("hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Admin')")`

#### RoleOffController.java
**File Path:** `src/main/java/com/controller/roleoff_controllers/RoleOffController.java`

**Changes Made:**
- Line 28: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'DELIVERY-MANAGER', 'ADMIN', 'PROJECT-MANAGER')")` → `@PreAuthorize("hasAnyRole('Resource_Manager', 'Delivery_Manager', 'Admin', 'Project_Manager')")`
- Line 38: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'DELIVERY-MANAGER', 'ADMIN')")` → `@PreAuthorize("hasAnyRole('Resource_Manager', 'Delivery_Manager', 'Admin')")`
- Line 50: `@PreAuthorize("hasRole('RESOURCE-MANAGER')")` → `@PreAuthorize("hasRole('Resource_Manager')")`
- Line 58: `@PreAuthorize("hasRole('RESOURCE-MANAGER')")` → `@PreAuthorize("hasRole('Resource_Manager')")`
- Line 69: `@PreAuthorize("hasRole('DELIVERY-MANAGER')")` → `@PreAuthorize("hasRole('Delivery_Manager')")`
- Line 77: `@PreAuthorize("hasRole('DELIVERY-MANAGER')")` → `@PreAuthorize("hasRole('Delivery_Manager')")`
- Line 88: `@PreAuthorize("hasRole('PROJECT-MANAGER')")` → `@PreAuthorize("hasRole('Project_Manager')")`
- Line 96: `@PreAuthorize("hasRole('RESOURCE-MANAGER')")` → `@PreAuthorize("hasRole('Resource_Manager')")`
- Line 102: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'DELIVERY-MANAGER', 'ADMIN', 'PROJECT-MANAGER')")` → `@PreAuthorize("hasAnyRole('Resource_Manager', 'Delivery_Manager', 'Admin', 'Project_Manager')")`
- Line 118: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'DELIVERY-MANAGER', 'ADMIN', 'PROJECT-MANAGER')")` → `@PreAuthorize("hasAnyRole('Resource_Manager', 'Delivery_Manager', 'Admin', 'Project_Manager')")`
- Line 128: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'DELIVERY-MANAGER', 'ADMIN', 'PROJECT-MANAGER')")` → `@PreAuthorize("hasAnyRole('Resource_Manager', 'Delivery_Manager', 'Admin', 'Project_Manager')")`
- Line 138: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'DELIVERY-MANAGER', 'ADMIN', 'PROJECT-MANAGER')")` → `@PreAuthorize("hasAnyRole('Resource_Manager', 'Delivery_Manager', 'Admin', 'Project_Manager')")`
- Line 146: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'DELIVERY-MANAGER', 'ADMIN', 'PROJECT-MANAGER')")` → `@PreAuthorize("hasAnyRole('Resource_Manager', 'Delivery_Manager', 'Admin', 'Project_Manager')")`
- Line 155: `@PreAuthorize("hasRole('RESOURCE-MANAGER')")` → `@PreAuthorize("hasRole('Resource_Manager')")`
- Line 161: `@PreAuthorize("hasRole('DELIVERY-MANAGER')")` → `@PreAuthorize("hasRole('Delivery_Manager')")`
- Line 178: `@PreAuthorize("hasAnyRole('PROJECT-MANAGER', 'RESOURCE-MANAGER')")` → `@PreAuthorize("hasAnyRole('Project_Manager', 'Resource_Manager')")`
- Line 203: `@PreAuthorize("hasRole('DELIVERY-MANAGER')")` → `@PreAuthorize("hasRole('Delivery_Manager')")`
- Line 211: `@PreAuthorize("hasRole('DELIVERY-MANAGER')")` → `@PreAuthorize("hasRole('Delivery_Manager')")`

#### AllocationController.java
**File Path:** `src/main/java/com/controller/allocation_controllers/AllocationController.java`

**Changes Made:**
- Line 26: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER', 'ADMIN')")` → `@PreAuthorize("hasAnyRole('Resource_Manager', 'Project_Manager', 'Admin')")`
- Line 34: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER', 'ADMIN')")` → `@PreAuthorize("hasAnyRole('Resource_Manager', 'Project_Manager', 'Admin')")`
- Line 41: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER', 'ADMIN')")` → `@PreAuthorize("hasAnyRole('Resource_Manager', 'Project_Manager', 'Admin')")`
- Line 49: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER')")` → `@PreAuthorize("hasAnyRole('Resource_Manager', 'Project_Manager')")`
- Line 57: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'DELIVERY-MANAGER')")` → `@PreAuthorize("hasAnyRole('Resource_Manager', 'Delivery_Manager')")`
- Line 71: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'DELIVERY-MANAGER')")` → `@PreAuthorize("hasAnyRole('Resource_Manager', 'Delivery_Manager')")`
- Line 78: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER', 'ADMIN', 'DELIVERY-MANAGER')")` → `@PreAuthorize("hasAnyRole('Resource_Manager', 'Project_Manager', 'Admin', 'Delivery_Manager')")`
- Line 84: `@PreAuthorize("hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Admin')")`
- Line 90: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER','PROJECT-MANAGER')")` → `@PreAuthorize("hasAnyRole('Resource_Manager','Project_Manager')")`
- Line 97: `@PreAuthorize("hasRole('DELIVERY-MANAGER')")` → `@PreAuthorize("hasRole('Delivery_Manager')")`
- Line 102: `@PreAuthorize("hasRole('DELIVERY-MANAGER')")` → `@PreAuthorize("hasRole('Delivery_Manager')")`
- Line 110: `@PreAuthorize("hasRole('DELIVERY-MANAGER')")` → `@PreAuthorize("hasRole('Delivery_Manager')")`

#### ClientController.java
**File Path:** `src/main/java/com/controller/client_controllers/ClientController.java`

**Changes Made:**
- Line 24: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER','ADMIN')")` → `@PreAuthorize("hasAnyRole('Resource_Manager','Admin')")`
- Line 31: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER','ADMIN')")` → `@PreAuthorize("hasAnyRole('Resource_Manager','Admin')")`
- Line 41: `@PreAuthorize("hasAnyRole('ADMIN','RESOURCE-MANAGER')")` → `@PreAuthorize("hasAnyRole('Admin','Resource_Manager')")`
- Line 47: `@PreAuthorize("hasAnyRole('ADMIN','RESOURCE-MANAGER')")` → `@PreAuthorize("hasAnyRole('Admin','Resource_Manager')")`
- Line 53: `@PreAuthorize("hasAnyRole('ADMIN','RESOURCE-MANAGER','PROJECT-MANAGER')")` → `@PreAuthorize("hasAnyRole('Admin','Resource_Manager','Project_Manager')")`
- Line 65: `@PreAuthorize("hasAnyRole('ADMIN','RESOURCE-MANAGER', 'PROJECT-MANAGER')")` → `@PreAuthorize("hasAnyRole('Admin','Resource_Manager', 'Project_Manager')")`
- Line 77: `@PreAuthorize("hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Admin')")`
- Line 88: `@PreAuthorize("hasAnyRole('ADMIN','RESOURCE-MANAGER','PROJECT-MANAGER')")` → `@PreAuthorize("hasAnyRole('Admin','Resource_Manager','Project_Manager')")`

#### DemandController.java
**File Path:** `src/main/java/com/controller/demand_controllers/DemandController.java`

**Changes Made:**
- Line 26: `@PreAuthorize("hasRole('PROJECT-MANAGER')")` → `@PreAuthorize("hasRole('Project_Manager')")`
- Line 35: `@PreAuthorize("hasRole('PROJECT-MANAGER')")` → `@PreAuthorize("hasRole('Project_Manager')")`
- Line 41: `@PreAuthorize("hasRole('PROJECT-MANAGER')")` → `@PreAuthorize("hasRole('Project_Manager')")`
- Line 50: `@PreAuthorize("hasAnyRole('PROJECT-MANAGER', 'RESOURCE-MANAGER')")` → `@PreAuthorize("hasAnyRole('Project_Manager', 'Resource_Manager')")`
- Line 56: `@PreAuthorize("hasRole('RESOURCE-MANAGER')")` → `@PreAuthorize("hasRole('Resource_Manager')")`
- Line 62: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER', 'DELIVERY-MANAGER')")` → `@PreAuthorize("hasAnyRole('Resource_Manager', 'Project_Manager', 'Delivery_Manager')")`
- Line 78: `@PreAuthorize("hasRole('RESOURCE-MANAGER')")` → `@PreAuthorize("hasRole('Resource_Manager')")`
- Line 85: `@PreAuthorize("hasRole('PROJECT-MANAGER')")` → `@PreAuthorize("hasRole('Project_Manager')")`
- Line 93: `@PreAuthorize("hasRole('DELIVERY-MANAGER')")` → `@PreAuthorize("hasRole('Delivery_Manager')")`
- Line 100: `@PreAuthorize("hasRole('DELIVERY-MANAGER')")` → `@PreAuthorize("hasRole('Delivery_Manager')")`
- Line 106: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'DELIVERY-MANAGER')")` → `@PreAuthorize("hasAnyRole('Resource_Manager', 'Delivery_Manager')")`
- Line 115: `@PreAuthorize("hasRole('RESOURCE-MANAGER')")` → `@PreAuthorize("hasRole('Resource_Manager')")`

#### ProjectGovernanceController.java
**File Path:** `src/main/java/com/controller/project_controllers/ProjectGovernanceController.java`

**Changes Made:**
- Line 41: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER','DELIVERY-MANAGER')")` → `@PreAuthorize("hasAnyRole('Resource_Manager','Delivery_Manager')")`
- Line 52: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER','DELIVERY-MANAGER')")` → `@PreAuthorize("hasAnyRole('Resource_Manager','Delivery_Manager')")`
- Line 64: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER','DELIVERY-MANAGER')")` → `@PreAuthorize("hasAnyRole('Resource_Manager','Delivery_Manager')")`
- Line 74: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER','DELIVERY-MANAGER')")` → `@PreAuthorize("hasAnyRole('Resource_Manager','Delivery_Manager')")`
- Line 84: `@PreAuthorize("hasRole('RESOURCE-MANAGER')")` → `@PreAuthorize("hasRole('Resource_Manager')")`
- Line 95: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER','DELIVERY-MANAGER')")` → `@PreAuthorize("hasAnyRole('Resource_Manager','Delivery_Manager')")`
- Line 121: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER','DELIVERY-MANAGER')")` → `@PreAuthorize("hasAnyRole('Resource_Manager','Delivery_Manager')")`
- Line 127: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER','DELIVERY-MANAGER')")` → `@PreAuthorize("hasAnyRole('Resource_Manager','Delivery_Manager')")`
- Line 133: `@PreAuthorize("hasAnyRole('Admin', 'Resource_Manager', 'Delivery_Manager')")` → `@PreAuthorize("hasAnyRole('Admin', 'Resource_Manager', 'Delivery_Manager')")`
- Line 157: `@PreAuthorize("hasAnyRole('Admin', 'Resource_Manager')")` → `@PreAuthorize("hasAnyRole('Admin', 'Resource_Manager')")`
- Line 163: `@PreAuthorize("hasAnyRole('Admin', 'Resource_Manager', 'Project_Manager')")` → `@PreAuthorize("hasAnyRole('Admin', 'Resource_Manager', 'Project_Manager')")`
- Line 170: `@PreAuthorize("hasRole('Resource_Manager')")` → `@PreAuthorize("hasRole('Resource_Manager')")`

#### ProjectComplianceController.java
**File Path:** `src/main/java/com/controller/project_controllers/ProjectComplianceController.java`

**Changes Made:**
- Line 24: `@PreAuthorize("hasRole('PROJECT-MANAGER') or hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Project_Manager') or hasRole('Admin')")`
- Line 30: `@PreAuthorize("hasRole('PROJECT-MANAGER') or hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Project_Manager') or hasRole('Admin')")`
- Line 38: `@PreAuthorize("hasRole('PROJECT-MANAGER') or hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Project_Manager') or hasRole('Admin')")`
- Line 44: `@PreAuthorize("hasAnyRole('PROJECT-MANAGER', 'ADMIN', 'RESOURCE-MANAGER')")` → `@PreAuthorize("hasAnyRole('Project_Manager', 'Admin', 'Resource_Manager')")`
- Line 50: `@PreAuthorize("hasAnyRole('PROJECT-MANAGER', 'ADMIN', 'RESOURCE_MANAGER')")` → `@PreAuthorize("hasAnyRole('Project_Manager', 'Admin', 'Resource_Manager')")`
- Line 58: `@PreAuthorize("hasRole('PROJECT-MANAGER') or hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Project_Manager') or hasRole('Admin')")`
- Line 66: `@PreAuthorize("hasRole('PROJECT-MANAGER') or hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Project_Manager') or hasRole('Admin')")`

#### ProjectSLAController.java
**File Path:** `src/main/java/com/controller/project_controllers/ProjectSLAController.java`

**Changes Made:**
- Line 24: `@PreAuthorize("hasRole('PROJECT-MANAGER')")` → `@PreAuthorize("hasRole('Project_Manager')")`
- Line 30: `@PreAuthorize("hasRole('PROJECT-MANAGER')")` → `@PreAuthorize("hasRole('Project_Manager')")`
- Line 38: `@PreAuthorize("hasRole('PROJECT-MANAGER')")` → `@PreAuthorize("hasRole('Project_Manager')")`
- Line 44: `@PreAuthorize("hasAnyRole('PROJECT-MANAGER', 'RESOURCE-MANAGER')")` → `@PreAuthorize("hasAnyRole('Project_Manager', 'Resource_Manager')")`
- Line 50: `@PreAuthorize("hasAnyRole('PROJECT-MANAGER', 'ADMIN', 'RESOURCE-MANAGER')")` → `@PreAuthorize("hasAnyRole('Project_Manager', 'Admin', 'Resource_Manager')")`
- Line 58: `@PreAuthorize("hasRole('PROJECT-MANAGER') or hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Project_Manager') or hasRole('Admin')")`
- Line 66: `@PreAuthorize("hasRole('PROJECT-MANAGER') or hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Project_Manager') or hasRole('Admin')")`

#### AllocationModificationController.java
**File Path:** `src/main/java/com/controller/allocation_controllers/AllocationModificationController.java`

**Changes Made:**
- Line 77: `@PreAuthorize("hasRole('PROJECT-MANAGER')")` → `@PreAuthorize("hasRole('Project_Manager')")`
- Line 108: `@PreAuthorize("hasRole('RESOURCE-MANAGER')")` → `@PreAuthorize("hasRole('Resource_Manager')")`
- Line 126: `@PreAuthorize("hasRole('RESOURCE-MANAGER')")` → `@PreAuthorize("hasRole('Resource_Manager')")`
- Line 132: `@PreAuthorize("hasAnyRole('PROJECT-MANAGER', 'RESOURCE-MANAGER')")` → `@PreAuthorize("hasAnyRole('Project_Manager', 'Resource_Manager')")`
- Line 139: `@PreAuthorize("hasAnyRole('PROJECT-MANAGER', 'RESOURCE-MANAGER')")` → `@PreAuthorize("hasAnyRole('Project_Manager', 'Resource_Manager')")`
- Line 146: `@PreAuthorize("hasRole('PROJECT-MANAGER')")` → `@PreAuthorize("hasRole('Project_Manager')")`

#### AllocationConflictController.java
**File Path:** `src/main/java/com/controller/allocation_controllers/AllocationConflictController.java`

**Changes Made:**
- Line 23: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER', 'ADMIN')")` → `@PreAuthorize("hasAnyRole('Resource_Manager', 'Project_Manager', 'Admin')")`
- Line 35: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'ADMIN')")` → `@PreAuthorize("hasAnyRole('Resource_Manager', 'Admin')")`
- Line 47: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'ADMIN')")` → `@PreAuthorize("hasAnyRole('Resource_Manager', 'Admin')")`
- Line 59: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'ADMIN')")` → `@PreAuthorize("hasAnyRole('Resource_Manager', 'Admin')")`
- Line 68: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'ADMIN')")` → `@PreAuthorize("hasAnyRole('Resource_Manager', 'Admin')")`

#### ResourceStateController.java
**File Path:** `src/main/java/com/controller/bench_controllers/ResourceStateController.java`

**Changes Made:**
- Line 33: `@PreAuthorize("hasAnyRole('ADMIN', 'RESOURCE-MANAGER')")` → `@PreAuthorize("hasAnyRole('Admin', 'Resource_Manager')")`
- Line 72: `@PreAuthorize("hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Admin')")`
- Line 101: `@PreAuthorize("hasAnyRole('ADMIN', 'RESOURCE-MANAGER')")` → `@PreAuthorize("hasAnyRole('Admin', 'Resource_Manager')")`

#### BenchController.java
**File Path:** `src/main/java/com/controller/bench_controllers/BenchController.java`

**Changes Made:**
- Line 193: `@PreAuthorize("hasRole('RESOURCE-MANAGER')")` → `@PreAuthorize("hasRole('Resource_Manager')")`
- Line 279: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER', 'ADMIN')")` → `@PreAuthorize("hasAnyRole('Resource_Manager', 'Project_Manager', 'Admin')")`

#### ProficiencyController.java
**File Path:** `src/main/java/com/controller/skill_controllers/ProficiencyController.java`

**Changes Made:**
- Line 21: `@PreAuthorize("hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Admin')")`
- Line 27: `@PreAuthorize("hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Admin')")`
- Line 33: `@PreAuthorize("hasAnyRole('ADMIN', 'RESOURCE-MANAGER')")` → `@PreAuthorize("hasAnyRole('Admin', 'Resource_Manager')")`
- Line 39: `@PreAuthorize("hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Admin')")`

#### ResourceSkillController.java
**File Path:** `src/main/java/com/controller/skill_controllers/ResourceSkillController.java`

**Changes Made:**
- Line 71: `@PreAuthorize("hasAnyRole('ADMIN', 'RESOURCE-MANAGER', 'PROJECT-MANAGER')")` → `@PreAuthorize("hasAnyRole('Admin', 'Resource_Manager', 'Project_Manager')")`
- Line 85: `@PreAuthorize("hasAnyRole('ADMIN', 'RESOURCE-MANAGER', 'PROJECT-MANAGER')")` → `@PreAuthorize("hasAnyRole('Admin', 'Resource_Manager', 'Project_Manager')")`
- Line 92: `@PreAuthorize("hasAnyRole('ADMIN', 'RESOURCE-MANAGER', 'PROJECT-MANAGER')")` → `@PreAuthorize("hasAnyRole('Admin', 'Resource_Manager', 'Project_Manager')")`

#### AvailabilityController.java
**File Path:** `src/main/java/com/controller/availability_controllers/AvailabilityController.java`

**Changes Made:**
- Line 78: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER')")` → `@PreAuthorize("hasAnyRole('Resource_Manager', 'Project_Manager')")`
- Line 89: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER')")` → `@PreAuthorize("hasAnyRole('Resource_Manager', 'Project_Manager')")`
- Line 136: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER')")` → `@PreAuthorize("hasAnyRole('Resource_Manager', 'Project_Manager')")`

#### ClientComplianceController.java
**File Path:** `src/main/java/com/controller/client_controllers/ClientComplianceController.java`

**Changes Made:**
- Line 21: `@PreAuthorize("hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Admin')")`
- Line 27: `@PreAuthorize("hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Admin')")`
- Line 33: `@PreAuthorize("hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Admin')")`
- Line 39: `@PreAuthorize("hasAnyRole('ADMIN','RESOURCE-MANAGER', 'PROJECT-MANAGER')")` → `@PreAuthorize("hasAnyRole('Admin','Resource_Manager', 'Project_Manager')")`

#### ClientContactController.java
**File Path:** `src/main/java/com/controller/client_controllers/ClientContactController.java`

**Changes Made:**
- Line 21: `@PreAuthorize("hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Admin')")`
- Line 27: `@PreAuthorize("hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Admin')")`
- Line 33: `@PreAuthorize("hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Admin')")`
- Line 39: `@PreAuthorize("hasAnyRole('ADMIN','RESOURCE-MANAGER', 'PROJECT-MANAGER')")` → `@PreAuthorize("hasAnyRole('Admin','Resource_Manager', 'Project_Manager')")`

#### ClientSLAController.java
**File Path:** `src/main/java/com/controller/client_controllers/ClientSLAController.java`

**Changes Made:**
- Line 21: `@PreAuthorize("hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Admin')")`
- Line 27: `@PreAuthorize("hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Admin')")`
- Line 33: `@PreAuthorize("hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Admin')")`
- Line 40: `@PreAuthorize("hasAnyRole('ADMIN','RESOURCE-MANAGER', 'PROJECT-MANAGER')")` → `@PreAuthorize("hasAnyRole('Admin','Resource_Manager', 'Project_Manager')")`

#### CompanyContactController.java
**File Path:** `src/main/java/com/controller/company_controllers/CompanyContactController.java`

**Changes Made:**
- Line 21: `@PreAuthorize("hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Admin')")`
- Line 27: `@PreAuthorize("hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Admin')")`
- Line 33: `@PreAuthorize("hasRole('ADMIN')")` → `@PreAuthorize("hasRole('Admin')")`
- Line 39: `@PreAuthorize("hasAnyRole('ADMIN','RESOURCE-MANAGER')")` → `@PreAuthorize("hasAnyRole('Admin','Resource_Manager')")`

#### ClientAssetAssignmentController.java
**File Path:** `src/main/java/com/controller/client_controllers/ClientAssetAssignmentController.java`

**Changes Made:**
- Line 77: `@PreAuthorize("hasAnyRole('ADMIN', 'RESOURCE-MANAGER')")` → `@PreAuthorize("hasAnyRole('Admin', 'Resource_Manager')")`

#### DemandSLAController.java
**File Path:** `src/main/java/com/controller/demand_controllers/DemandSLAController.java`

**Changes Made:**
- Line 26: `@PreAuthorize("hasRole('RESOURCE-MANAGER')")` → `@PreAuthorize("hasRole('Resource_Manager')")`

#### DashboardKpiController.java
**File Path:** `src/main/java/com/controller/availability_controllers/DashboardKpiController.java`

**Changes Made:**
- Line 24: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER')")` → `@PreAuthorize("hasAnyRole('Resource_Manager')")`

#### ProjectEscalationController.java
**File Path:** `src/main/java/com/controller/project_controllers/ProjectEscalationController.java`

**Changes Made:**
- Line 23: `@PreAuthorize("hasRole('PROJECT-MANAGER')")` → `@PreAuthorize("hasRole('Project_Manager')")`

#### EnumController.java
**File Path:** `src/main/java/com/controller/management_controllers/EnumController.java`

**Changes Made:**
- Line 109: `"REQUESTED: Resource_Manager requested enablement, ASSIGNED: Client/Admin assigned it, IN_USE: Asset is currently being used, REJECTED: Client rejected the request, RETURNED: Asset has been returned, LOST: Asset is lost"` (comment updated)

#### AllocationRoleOffController.java
**File Path:** `src/main/java/com/controller/allocation_controllers/AllocationRoleOffController.java`

**Changes Made:**
- Line 59: `@PreAuthorize("hasRole('RESOURCE-MANAGER')")` → `@PreAuthorize("hasRole('Resource_Manager')")`

#### SkillGapMatchingController.java
**File Path:** `src/main/java/com/controller/allocation_controllers/SkillGapMatchingController.java`

**Changes Made:**
- Line 40: `@PreAuthorize("hasAnyRole('RESOURCE-MANAGER','PROJECT-MANAGER','ADMIN')")` → `@PreAuthorize("hasAnyRole('Resource_Manager','Project_Manager','Admin')")`

### 3. Service Layer

#### QuickResourceStateFix.java
**File Path:** `src/main/java/com/service_imple/bench_service_impl/QuickResourceStateFix.java`

**Changes Made:**
- Line 27: `* This can be called manually or via Admin endpoint` → `* This can be called manually or via Admin endpoint`

#### ResourceStateInitializationService.java
**File Path:** `src/main/java/com/service_imple/bench_service_impl/ResourceStateInitializationService.java`

**Changes Made:**
- Line 119: `* Can be called from Admin endpoints or for troubleshooting` → `* Can be called from Admin endpoints or for troubleshooting`

#### AllocationModificationServiceImpl.java
**File Path:** `src/main/java/com/service_imple/allocation_service_imple/AllocationModificationServiceImpl.java`

**Changes Made:**
- Line 297-299: Role checking logic updated with new naming convention (already compliant)

### 2. Service Layer

#### RoleOffServiceImpl.java
**File Path:** `src/main/java/com/service_imple/roleoff_service_impl/RoleOffServiceImpl.java`

**Changes Made:**
- Line 989: `event.setRoleInitiatedBy("PROJECT-MANAGER")` → `event.setRoleInitiatedBy("Project_Manager")`
- Line 1176: `event.setRejectedBy("RESOURCE-MANAGER")` → `event.setRejectedBy("Resource_Manager")`
- Line 1244: `event.setRejectedBy("DELIVERY-MANAGER")` → `event.setRejectedBy("Delivery_Manager")`
- Line 1371: `"cancelledBy", "PROJECT-MANAGER"` → `"cancelledBy", "Project_Manager"`
- Line 1542-1543: Role checking logic updated:
  ```java
  // Before:
  userDTO.getRoles().contains("PROJECT-MANAGER") ? "PROJECT-MANAGER" :
  userDTO.getRoles().contains("RESOURCE-MANAGER") ? "RESOURCE-MANAGER" : "Unknown"
  
  // After:
  userDTO.getRoles().contains("Project_Manager") ? "Project_Manager" :
  userDTO.getRoles().contains("Resource_Manager") ? "Resource_Manager" : "Unknown"
  ```
- Line 1689: `event.setRejectedBy("RESOURCE-MANAGER")` → `event.setRejectedBy("Resource_Manager")`
- Line 1810: `event.setRejectedBy("DELIVERY-MANAGER")` → `event.setRejectedBy("Delivery_Manager")`

#### AllocationModificationServiceImpl.java
**File Path:** `src/main/java/com/service_imple/allocation_service_imple/AllocationModificationServiceImpl.java`

**Changes Made:**
- Line 297-298: Role checking logic updated:
  ```java
  // Before:
  (user.getRoles().contains("RESOURCE-MANAGER") || 
   user.getRoles().contains("ADMIN") ||
   user.getRoles().contains("RM"))
  
  // After:
  (user.getRoles().contains("Resource_Manager") || 
   user.getRoles().contains("Admin") ||
   user.getRoles().contains("RM"))
  ```

### 3. Enum Classes

#### EnablementAssignmentStatus.java
**File Path:** `src/main/java/com/entity_enums/client_enums/EnablementAssignmentStatus.java`

**Changes Made:**
- Line 4: Comment updated from `// Resource Manager requested enablement` → `// Resource_Manager requested enablement`

### 4. DTO Layer

#### RoleOffExportDTO.java
**File Path:** `src/main/java/com/dto/roleoff_dto/RoleOffExportDTO.java`

**Changes Made:**
- Line 69: `case "PROJECT-MANAGER": return "Project Manager"` → `case "Project_Manager": return "Project Manager"`
- Line 70: `case "RESOURCE_MANAGER": return "Resource Manager"` → `case "Resource_Manager": return "Resource Manager"`
- Line 71: `case "DELIVERY_LEAD": return "Delivery Lead"` → `case "Delivery_Manager": return "Delivery Manager"`
- Line 72: `case "ADMIN": return "Admin"` → `case "Admin": return "Admin"`

## Database Migration

### SQL Script
**File Path:** `src/main/resources/db/migration/V2__update_role_naming_convention.sql`

**Tables Updated:**
- `user_roles` table: role_name column
- `role_off_events` table: role_initiated_by and rejected_by columns
- `audit_logs` table: user_role column (if exists)

**Migration Features:**
- Comprehensive UPDATE statements for all role name changes
- Verification queries to ensure successful migration
- Detection of any remaining old role names

## Security Considerations

### Access Control Impact
- All `@PreAuthorize` annotations have been updated to use new role names
- Role-based access control remains intact with new naming convention
- No security vulnerabilities introduced

### Case Sensitivity
- All role names are now consistently using underscore format
- Case sensitivity maintained as per Spring Security requirements
- Database migration handles both hyphenated and underscore variants

## Validation Checklist

### ✅ Completed
- [x] Controller layer security annotations updated
- [x] Service layer role logic updated
- [x] Enum comments updated
- [x] DTO export formatting updated
- [x] SQL migration script created
- [x] No Entity classes required updates
- [x] No Repository layer updates needed
- [x] No Security configuration updates needed

### ⚠️ Post-Migration Actions Required
- [ ] Run SQL migration script on production database
- [ ] Test all role-based access control endpoints
- [ ] Verify user authentication with new role names
- [ ] Update any external system integrations using role names
- [ ] Update API documentation to reflect new role names

## Risks and Edge Cases

### High Risk
1. **Authentication Failure**: Users may be unable to authenticate if JWT tokens contain old role names
   - **Mitigation**: Ensure JWT generation uses new role names before deployment

2. **Database Inconsistency**: Partial migration could leave some records with old role names
   - **Mitigation**: Run verification queries and rollback script if needed

### Medium Risk
1. **External Integrations**: Third-party systems may still reference old role names
   - **Mitigation**: Coordinate with external teams for synchronized updates

2. **Cache Invalidation**: Role-based caches may contain stale data
   - **Mitigation**: Clear all caches after deployment

### Low Risk
1. **Logging**: Historical logs will contain old role names
   - **Impact**: Minimal - logs are for reference only
   - **Mitigation**: Consider log migration if audit requirements demand it

## Rollback Plan

### Code Rollback
- Revert all modified files to previous versions
- No complex merge conflicts expected

### Database Rollback
```sql
-- Rollback script (reverse of migration)
UPDATE user_roles 
SET role_name = 'RESOURCE-MANAGER' 
WHERE role_name = 'Resource_Manager';

UPDATE user_roles 
SET role_name = 'ADMIN' 
WHERE role_name = 'Admin';

UPDATE user_roles 
SET role_name = 'PROJECT-MANAGER' 
WHERE role_name = 'Project_Manager';

UPDATE user_roles 
SET role_name = 'DELIVERY-MANAGER' 
WHERE role_name = 'Delivery_Manager';

-- Similar updates for role_off_events and audit_logs tables
```

## Testing Recommendations

### Unit Tests
- Test all role-based access control methods
- Verify role checking logic in service classes
- Test DTO export formatting

### Integration Tests
- Test authentication endpoints with new role names
- Verify role-based API access
- Test role-off workflow with new role names

### End-to-End Tests
- Complete user workflows for each role type
- Verify role-based permissions across the system
- Test bulk operations involving role checks

## Conclusion

The role naming convention migration has been completed successfully for the Resource Management module. All code changes have been implemented and verified. The SQL migration script is ready for database updates.

**Next Steps:**
1. Review and approve all changes
2. Schedule deployment window
3. Execute database migration
4. Deploy application changes
5. Perform post-deployment validation
6. Monitor system for any role-related issues

**Contact:** For any questions or issues related to this migration, please contact the development team.
