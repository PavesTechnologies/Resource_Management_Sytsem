package com.controller.management_controllers;

import com.dto.ApiResponse;
import com.dto.enum_dto.EnumSpecificationDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/enums")
@CrossOrigin
public class EnumController {

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<EnumSpecificationDTO>>> getAllEnums() {
        List<EnumSpecificationDTO> enums = new ArrayList<>();
        
        // CDC Mapping Enums
        enums.add(new EnumSpecificationDTO(
            "FieldType",
            "CDC Mapping",
            "Defines data types for field mapping in CDC operations",
            new String[]{"STRING", "LONG", "UUID", "ENUM", "BIG_DECIMAL", "LOCAL_DATE_TIME"},
            "Used for Change Data Capture field type definitions"
        ));

        // Allocation Enums
        enums.add(new EnumSpecificationDTO(
            "AllocationStatus",
            "Allocation",
            "Defines the status of resource allocations",
            new String[]{"PLANNED", "ACTIVE", "ENDED", "CANCELLED"},
            "PLANNED: Tentative allocation affecting projected availability, ACTIVE: Confirmed allocation affecting firm availability, ENDED: Historical allocation ignored in calculations, CANCELLED: Cancelled allocation ignored in calculations"
        ));

        // Centralised Enums
        enums.add(new EnumSpecificationDTO(
            "DeliveryModel",
            "Centralised",
            "Defines work delivery models",
            new String[]{"ONSITE", "OFFSHORE", "HYBRID"},
            "Used to specify where work is performed"
        ));

        enums.add(new EnumSpecificationDTO(
            "PriorityLevel",
            "Centralised",
            "Defines priority levels for various entities",
            new String[]{"LOW", "MEDIUM", "HIGH", "CRITICAL"},
            "Used for prioritizing demands, projects, and tasks"
        ));

        enums.add(new EnumSpecificationDTO(
            "RecordStatus",
            "Centralised",
            "Defines the status of records in the system",
            new String[]{"ACTIVE", "INACTIVE", "ON_HOLD"},
            "Used for managing record lifecycle"
        ));

        enums.add(new EnumSpecificationDTO(
            "RiskLevel",
            "Centralised",
            "Defines risk assessment levels",
            new String[]{"LOW", "MEDIUM", "HIGH", "CRITICAL"},
            "Used for risk evaluation and management"
        ));

        // Client Enums
        enums.add(new EnumSpecificationDTO(
            "AssetCategory",
            "Client",
            "Defines categories of client assets",
            new String[]{"DEVICE", "SOFTWARE", "ACCESS", "TOOLS"},
            "Used for categorizing client-provided assets"
        ));

        enums.add(new EnumSpecificationDTO(
            "AssetStatus",
            "Client",
            "Defines the status of client assets",
            new String[]{"ACTIVE", "RETIRED", "INACTIVE", "LOST"},
            "Used for tracking asset lifecycle"
        ));

        enums.add(new EnumSpecificationDTO(
            "ClientType",
            "Client",
            "Defines types of clients",
            new String[]{"STRATEGIC", "STANDARD", "SUPPORT", "INTERNAL"},
            "Used for client classification and management"
        ));

        enums.add(new EnumSpecificationDTO(
            "ContactRole",
            "Client",
            "Defines roles of client contacts",
            new String[]{"DELIVERY_HEAD", "DELIVERY_MANAGER", "COMPLIANCE_OFFICER", "PROJECT_MANAGER", "ACCOUNT_MANAGER", "TECHNICAL_LEAD"},
            "Used for defining contact responsibilities"
        ));

        enums.add(new EnumSpecificationDTO(
            "EnablementAssignmentStatus",
            "Client",
            "Defines status of resource enablement assignments",
            new String[]{"REQUESTED", "ASSIGNED", "IN_USE", "REJECTED", "RETURNED", "LOST"},
            "REQUESTED: Resource Manager requested enablement, ASSIGNED: Client/Admin assigned it, IN_USE: Asset is currently being used, REJECTED: Client rejected the request, RETURNED: Asset has been returned, LOST: Asset is lost"
        ));

        enums.add(new EnumSpecificationDTO(
            "RequirementType",
            "Client",
            "Defines types of client requirements",
            new String[]{"CERTIFICATION", "CLEARANCE", "TOOL_ACCESS", "SKILL"},
            "Used for categorizing resource requirements"
        ));

        enums.add(new EnumSpecificationDTO(
            "SLAType",
            "Client",
            "Defines types of Service Level Agreements",
            new String[]{"NET_NEW", "REPLACEMENT", "BACKFILL", "EMERGENCY"},
            "Used for SLA classification"
        ));

        // Demand Enums
        enums.add(new EnumSpecificationDTO(
            "DemandCommitment",
            "Demand",
            "Defines commitment levels for demands",
            new String[]{"SOFT", "CONFIRMED"},
            "Used to indicate demand certainty"
        ));

        enums.add(new EnumSpecificationDTO(
            "DemandStatus",
            "Demand",
            "Defines the status of demand requests",
            new String[]{"DRAFT", "REQUESTED", "APPROVED", "REJECTED", "CANCELLED"},
            "Used for demand lifecycle management"
        ));

        enums.add(new EnumSpecificationDTO(
            "DemandType",
            "Demand",
            "Defines types of demand requests",
            new String[]{"NET_NEW", "REPLACEMENT", "BACKFILL", "EMERGENCY"},
            "Used for categorizing demand nature"
        ));

        // Project Enums
        enums.add(new EnumSpecificationDTO(
            "EscalationLevel",
            "Project",
            "Defines escalation levels for project issues",
            new String[]{"LEVEL_1", "LEVEL_2", "EXECUTIVE"},
            "Used for issue escalation hierarchy"
        ));

        enums.add(new EnumSpecificationDTO(
            "EscalationSource",
            "Project",
            "Defines sources of escalations",
            new String[]{"INHERITED", "MANUAL"},
            "Used to track escalation origins"
        ));

        enums.add(new EnumSpecificationDTO(
            "EscalationTriggerType",
            "Project",
            "Defines types of escalation triggers",
            new String[]{"SLA_BREACH", "COMPLIANCE_FAILURE", "DELIVERY_RISK", "SECURITY_INCIDENT", "FINANCIAL_RISK"},
            "Used for categorizing escalation reasons"
        ));

        enums.add(new EnumSpecificationDTO(
            "ProjectDataStatus",
            "Project",
            "Defines status of project data",
            new String[]{"COMPLETE", "INCOMPLETE", "PENDING", "ERROR"},
            "Used for data completeness tracking"
        ));

        enums.add(new EnumSpecificationDTO(
            "ProjectStage",
            "Project",
            "Defines stages in project lifecycle",
            new String[]{"INITIATION", "PLANNING", "DESIGN", "DEVELOPMENT", "TESTING", "DEPLOYMENT", "MAINTENANCE", "COMPLETED", "MOBILIZATION", "EXECUTION", "STABILIZATION"},
            "Used for project progress tracking"
        ));

        enums.add(new EnumSpecificationDTO(
            "ProjectStatus",
            "Project",
            "Defines the overall status of projects",
            new String[]{"ACTIVE", "APPROVED", "ARCHIVED", "PLANNING", "COMPLETED"},
            "Used for project state management"
        ));

        enums.add(new EnumSpecificationDTO(
            "StaffingReadinessStatus",
            "Project",
            "Defines staffing readiness status",
            new String[]{"NOT_READY", "UPCOMING", "READY", "MISSING"},
            "Used for staffing preparation tracking"
        ));

        // Resource Enums
        enums.add(new EnumSpecificationDTO(
            "EmploymentStatus",
            "Resource",
            "Defines employment status of resources",
            new String[]{"ACTIVE", "ON_NOTICE", "EXITED"},
            "Used for resource employment tracking"
        ));

        enums.add(new EnumSpecificationDTO(
            "EmploymentType",
            "Resource",
            "Defines types of employment",
            new String[]{"FTE", "CONTRACTOR", "VENDOR"},
            "Used for employment classification"
        ));

        enums.add(new EnumSpecificationDTO(
            "WorkforceCategory",
            "Resource",
            "Defines workforce categories",
            new String[]{"INTERNAL", "VENDOR", "SHADOW"},
            "Used for workforce classification"
        ));

        enums.add(new EnumSpecificationDTO(
            "WorkingMode",
            "Resource",
            "Defines working modes for resources",
            new String[]{"WFO", "WFH", "HYBRID"},
            "WFO: Work From Office, WFH: Work From Home, HYBRID: Combination of both"
        ));

        // Skill Enums
        enums.add(new EnumSpecificationDTO(
            "CertificateStatus",
            "Skill",
            "Defines status of certifications",
            new String[]{"ACTIVE", "EXPIRING_SOON", "EXPIRED"},
            "Used for certification lifecycle management"
        ));

        enums.add(new EnumSpecificationDTO(
            "CertificateType",
            "Skill",
            "Defines types of certificates",
            new String[]{"SKILL_BASED", "ACHIEVEMENT"},
            "Used for certificate classification"
        ));

        enums.add(new EnumSpecificationDTO(
            "TemplateStatus",
            "Skill",
            "Defines status of skill templates",
            new String[]{"DRAFT", "ACTIVE", "INACTIVE"},
            "Used for template lifecycle management"
        ));

        return ResponseEntity.ok(new ApiResponse<>(true,"Success", enums));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<EnumSpecificationDTO>>> getEnumsByCategory(@PathVariable String category) {
        List<EnumSpecificationDTO> allEnums = getAllEnums().getBody().getData();
        List<EnumSpecificationDTO> filteredEnums = new ArrayList<>();
        
        for (EnumSpecificationDTO enumSpec : allEnums) {
            if (enumSpec.getCategory().equalsIgnoreCase(category)) {
                filteredEnums.add(enumSpec);
            }
        }
        
        return ResponseEntity.ok(new ApiResponse<>(true,"Success", filteredEnums));
    }
}
