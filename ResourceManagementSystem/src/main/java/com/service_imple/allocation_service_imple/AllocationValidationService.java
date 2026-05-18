package com.service_imple.allocation_service_imple;

import com.dto.allocation_dto.AllocationFailure;
import com.dto.allocation_dto.AllocationPreloadedData;
import com.dto.allocation_dto.AllocationRequestDTO;
import com.dto.allocation_dto.AllocationValidationResult;
import com.dto.allocation_dto.DemandProjectData;
import com.dto.allocation_dto.ConflictDetectionResult;
import com.entity.allocation_entities.ResourceAllocation;
import com.entity.bench.ResourceState;
import com.entity.demand_entities.Demand;
import com.entity.project_entities.Project;
import com.entity.resource_entities.Resource;
import com.entity.skill_entities.ResourceSkill;
import com.entity.skill_entities.ResourceCertificate;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.entity_enums.centralised_enums.RecordStatus;
import com.entity_enums.demand_enums.DemandCommitment;
import com.entity_enums.demand_enums.DemandStatus;
import com.entity_enums.project_enums.ProjectStatus;
import com.global_exception_handler.AllocationExceptionHandler;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.allocation_repo.AllocationModificationRepository;
import com.repo.bench_repo.ResourceStateRepository;
import com.repo.resource_repo.ResourceRepository;
import com.repo.demand_repo.DemandRepository;
import com.repo.project_repo.ProjectRepository;
import com.repo.skill_repo.ResourceSkillRepository;
import com.repo.skill_repo.ResourceCertificateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for validating allocation requests
 */
@Service
@RequiredArgsConstructor
public class AllocationValidationService {

    private final AllocationRepository allocationRepository;
    private final AllocationModificationRepository allocationModificationRepository;
    private final ResourceRepository resourceRepository;
    private final DemandRepository demandRepository;
    private final ProjectRepository projectRepository;
    private final ResourceSkillRepository resourceSkillRepository;
    private final ResourceCertificateRepository resourceCertificateRepository;
    private final AllocationConflictService conflictService;
    private final AllocationCapacityService capacityService;
    private final SkillGapAnalysisService skillGapService;
    private final ResourceStateRepository resourceStateRepository;

    /**
     * Validates basic allocation request parameters
     */
    public void validateRequest(AllocationRequestDTO request) {
        if (request == null) {
            throw new AllocationExceptionHandler(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "Allocation request cannot be null"
            );
        }
        
        if (request.getResourceId() == null || request.getResourceId().isEmpty()) {
            throw new AllocationExceptionHandler(
                HttpStatus.BAD_REQUEST,
                "INVALID_RESOURCES",
                "Resource IDs cannot be null or empty"
            );
        }
        
        if (request.getAllocationStartDate() == null || request.getAllocationEndDate() == null) {
            throw new AllocationExceptionHandler(
                HttpStatus.BAD_REQUEST,
                "INVALID_DATES",
                "Allocation start and end dates are required"
            );
        }
        
        if (request.getAllocationStartDate().isAfter(request.getAllocationEndDate())) {
            throw new AllocationExceptionHandler(
                HttpStatus.BAD_REQUEST,
                "INVALID_DATE_RANGE",
                "Start date cannot be after end date"
            );
        }
        
        if (request.getAllocationPercentage() <= 0 || request.getAllocationPercentage() > 100) {
            // Check if this is a special condition request for >100% allocation
            if (request.getAllocationPercentage() > 100) {

                    throw new AllocationExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "Allocation percent exceded",
                        "Allocation percentage above 100% requires special condition approval. Maximum allowed is 130% with approval."
                    );

            } else {
                throw new AllocationExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PERCENTAGE",
                    "Allocation percentage must be between 1 and 100 normally"
                );
            }
        }
    }

    /**
     * Validates demand or project existence and business rules
     */
    public DemandProjectData validateDemandOrProject(AllocationRequestDTO request) {
        Demand demand = null;
        Project project = null;
        
        if (request.getDemandId() != null) {
            Optional<Demand> demandOpt = demandRepository.findById(request.getDemandId());
            if (demandOpt.isEmpty()) {
                throw new AllocationExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "DEMAND_NOT_FOUND",
                    "Demand not found"
                );
            }
            
            demand = demandOpt.get();
            project = demand.getProject();
            
            if (demand.getDemandCommitment().equals(DemandCommitment.SOFT)) {
                throw new AllocationExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "SOFT_COMMITMENT",
                    "Demand Commitment is SOFT. Allocation not allowed."
                );
            }
            
            // Skip demand status validation for quick allocation
            if (!request.getSkipValidation() && demand.getDemandStatus() != DemandStatus.APPROVED) {
                throw new AllocationExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "DEMAND_NOT_APPROVED",
                    "Demand Status is not APPROVED. Allocation not allowed."
                );
            }
            
            // Validate total allocations don't exceed demand's required resources
            int providedResources = request.getResourceId() != null ? request.getResourceId().size() : 0;
            int requiredResources = demand.getResourcesRequired();
            
            // Fetch existing allocations for this demand
            List<ResourceAllocation> existingAllocations = allocationRepository.findByDemand_DemandId(demand.getDemandId());
            
            // Count only active/planned allocations (exclude ended and cancelled)
            int existingActiveAllocations = (int) existingAllocations.stream()
                .filter(alloc -> alloc.getAllocationStatus() != AllocationStatus.ENDED && 
                               alloc.getAllocationStatus() != AllocationStatus.CANCELLED)
                .count();
            
            int totalAllocations = existingActiveAllocations + providedResources;
            
            if (totalAllocations > requiredResources) {
                throw new AllocationExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "DEMAND_ALLOCATION_EXCEEDED",
                    String.format("Demand allocation exceeded: Demand '%s' requires %d resources, already has %d allocated, requesting %d more (total: %d)", 
                                 demand.getDemandName(), requiredResources, existingActiveAllocations, 
                                 providedResources, totalAllocations)
                );
            }
        } else if (request.getProjectId() != null) {
            Optional<Project> projectOpt = projectRepository.findById(request.getProjectId());
            if (projectOpt.isEmpty()) {
                throw new AllocationExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "PROJECT_NOT_FOUND",
                    "Project not found"
                );
            }
            project = projectOpt.get();
        }

        return new DemandProjectData(demand, project);
    }

    /**
     * Preloads all allocation data to prevent N+1 queries during parallel validation
     */
    public AllocationPreloadedData preloadAllocationData(AllocationRequestDTO request, Demand demand) {
        List<String> resourceIds = request.getResourceId();
        
        // 1. Batch fetch all resources
        List<Resource> resources = resourceRepository.findAllById(resourceIds);
        Map<String, Resource> resourceMap = resources.stream()
                .collect(Collectors.toMap(Resource::getResourceId, r -> r));
        
        // 2. Batch fetch conflicting allocations for all resources
        List<ResourceAllocation> conflictingAllocations = allocationRepository.findConflictingAllocationsForResources(
                resourceIds, 
                request.getAllocationStartDate(), 
                request.getAllocationEndDate()
        );
        Map<String, List<ResourceAllocation>> allocationsByResource = conflictingAllocations.stream()
                .collect(Collectors.groupingBy(ra -> ra.getResource().getResourceId()));
        
        // 3. Batch fetch resource skills if demand exists
        Map<String, List<ResourceSkill>> skillsByResource = new HashMap<>();
        if (demand != null && demand.getRequiredSkills() != null && !demand.getRequiredSkills().isEmpty()) {
            List<ResourceSkill> allResourceSkills = resourceSkillRepository.findByResourceIdInAndActiveFlagTrue(resourceIds);
            skillsByResource = allResourceSkills.stream()
                    .collect(Collectors.groupingBy(ResourceSkill::getResourceId));
        }
        
        // 4. Batch fetch resource certificates if demand exists
        Map<String, List<ResourceCertificate>> certificatesByResource = new HashMap<>();
        if (demand != null && demand.getRequiredCertificates() != null && !demand.getRequiredCertificates().isEmpty()) {
            List<ResourceCertificate> allResourceCertificates = resourceCertificateRepository.findCertificatesForResources(
                    resourceIds, java.time.LocalDate.now());
            certificatesByResource = allResourceCertificates.stream()
                    .collect(Collectors.groupingBy(ResourceCertificate::getResourceId));
        }
        
        return new AllocationPreloadedData(resourceMap, allocationsByResource, skillsByResource, certificatesByResource);
    }

    /**
     * Validates resources in parallel using preloaded data
     */
    public AllocationValidationResult validateResourcesInParallel(
            AllocationRequestDTO request, 
            DemandProjectData demandProjectData, 
            AllocationPreloadedData preloadedData) {
        
        List<ResourceAllocation> validAllocations = Collections.synchronizedList(new ArrayList<>());
        List<AllocationFailure> failures = Collections.synchronizedList(new ArrayList<>());
        
        // Create final copies for lambda expression access
        final Demand finalDemand = demandProjectData.getDemand();
        final Project finalProject = demandProjectData.getProject();
        
        // Sequential resource validation using preloaded data to avoid Hibernate concurrency issues
        request.getResourceId().forEach(resourceId -> {
            try {
                // Validate resource existence and eligibility
                Resource resource = validateResource(resourceId, preloadedData.getResourceMap());
                if (resource == null) {
                    failures.add(new AllocationFailure(resourceId, getResourceName(resourceId, preloadedData.getResourceMap()), "Resource not found"));
                    return;
                }
                
                // Validate demand rules and priority conflicts
                validateDemandRules(request, finalDemand, finalProject, resourceId, preloadedData);

                boolean override = false;

                if (request.getAllocationStatus() == AllocationStatus.ACTIVE ||
                        request.getAllocationStatus() == AllocationStatus.PLANNED) {
                    // 🔹 CONSOLIDATED CAPACITY VALIDATION
                    override = validateCapacity(resourceId, request, preloadedData);
                }

                // Skip only skill compliance validation if skipValidation is true
                if (!Boolean.TRUE.equals(request.getSkipValidation()) && 
                    (request.getAllocationStatus() == AllocationStatus.ACTIVE || 
                     request.getAllocationStatus() == AllocationStatus.PLANNED)) {
                    
                    // Validate skill compliance (applies to both ACTIVE and PLANNED)
                    validateSkillCompliance(resourceId, finalDemand, request);
                }
                
                // Validate that at least one of demand or project is present
                if (finalDemand == null && finalProject == null) {
                    throw new AllocationExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "MISSING_DEMAND_OR_PROJECT",
                        "Allocation must have either a demand or project associated with it"
                    );
                }
                
                // Create allocation object if all validations pass
                ResourceAllocation allocation = new ResourceAllocation();
                allocation.setResource(resource);
                allocation.setDemand(finalDemand);
                allocation.setProject(finalProject);
                allocation.setAllocationStartDate(request.getAllocationStartDate());
                allocation.setAllocationEndDate(request.getAllocationEndDate());
                allocation.setAllocationPercentage(request.getAllocationPercentage());
                allocation.setAllocationStatus(request.getAllocationStatus());
                allocation.setCreatedBy(request.getCreatedBy());
                allocation.setRequestBeyondCapacityApproval(request.getRequestBeyondCapacityApproval());
                
                validAllocations.add(allocation);
                
            } catch (Exception e) {
                failures.add(new AllocationFailure(resourceId, getResourceName(resourceId, preloadedData.getResourceMap()), e.getMessage()));
            }
        });
        
        return new AllocationValidationResult(validAllocations, failures);
    }

    /**
     * Gets resource name from preloaded data map
     */
    private String getResourceName(String resourceId, Map<String, Resource> resourceMap) {
        Resource resource = resourceMap.get(resourceId);
        return resource != null ? resource.getFullName() : "Resource " + resourceId;
    }

    /**
     * Validates resource existence and eligibility
     */
    private Resource validateResource(String resourceId, Map<String, Resource> resourceMap) {
        Resource resource = resourceMap.get(resourceId);
        if (resource == null) {
            return null;
        }
        
        if (resource.getActiveFlag() == null || !resource.getActiveFlag()) {
            throw new AllocationExceptionHandler(
                HttpStatus.BAD_REQUEST,
                "RESOURCE_INELIGIBLE",
                "Resource is not active"
            );
        }
        return resource;
    }

    /**
     * Validates demand rules and detects priority conflicts
     */
    private void validateDemandRules(
            AllocationRequestDTO request, 
            Demand demand, 
            Project project, 
            String resourceId, 
            AllocationPreloadedData preloadedData) {
        
        // Validate demand exists and is approved (already done in validateDemandOrProject)
        if (demand != null && demand.getDemandCommitment().equals(DemandCommitment.SOFT)) {
            throw new AllocationExceptionHandler(
                HttpStatus.BAD_REQUEST,
                "SOFT_COMMITMENT",
                "Demand Commitment is SOFT. Allocation not allowed."
            );
        }
        
        // Validate project exists if project allocation
        if (demand == null && project == null) {
            throw new AllocationExceptionHandler(
                HttpStatus.BAD_REQUEST,
                "NO_DEMAND_OR_PROJECT",
                "Either demand or project must be specified"
            );
        }
        
        // Validate client and project status (applies to all allocations)
        validateClientAndProjectStatus(demand, project);
        
        // Priority conflict detection using preloaded allocations
        List<ResourceAllocation> existingAllocations = preloadedData.getAllocationsByResource().getOrDefault(resourceId, new ArrayList<>());
        ConflictDetectionResult conflictResult = conflictService.detectPriorityConflictsOptimized(
                request, resourceId, existingAllocations);

        if (conflictResult.isHasConflicts()) {

            if (request.getOverrideJustification() == null ||
                    request.getOverrideJustification().isBlank()) {

                throw new AllocationExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "PRIORITY_CONFLICT",
                        "Priority conflict detected: " + conflictResult.getMessage()
                );
            }

            // Allow override when justification is provided
        }
    }

    /**
     * Validates skill compliance using existing methods
     */
    private void validateSkillCompliance(String resourceId, Demand demand, AllocationRequestDTO request) {
        if (demand == null) {
            return; // Skip skill validation for project allocations
        }
        
        // Perform skill gap analysis and check if allocation is allowed
        var skillGapResult = skillGapService.performSkillGapAnalysis(demand, resourceId);
        
        // Check if allocation is allowed based on skill gap analysis
        if (!skillGapResult.getAllocationAllowed()) {
            throw new AllocationExceptionHandler(
                HttpStatus.BAD_REQUEST,
                "SKILL_GAP_MISMATCH",
                String.format("Resource %s does not meet skill requirements for demand '%s'. Match: %.1f%%, Risk: %s", 
                             resourceId, demand.getDemandName(), skillGapResult.getMatchPercentage(), skillGapResult.getRiskLevel())
            );
        }
        
        // Additional validation for mandatory requirements
        if (skillGapResult.getMatchPercentage() < 50.0) {
            throw new AllocationExceptionHandler(
                HttpStatus.BAD_REQUEST,
                "LOW_SKILL_MATCH",
                String.format("Resource %s has low skill match (%.1f%%) for demand '%s'. Minimum 50%% required", 
                             resourceId, skillGapResult.getMatchPercentage(), demand.getDemandName())
            );
        }
        
        // Reuse existing allocation requirements validation as additional safety check
        skillGapService.validateAllocationRequirements(resourceId, demand, request);
    }

    /**
     * Validates capacity using timeline segmentation algorithm with internal pool consideration
     */
    public boolean validateCapacity(String resourceId,
                                     AllocationRequestDTO request,
                                     AllocationPreloadedData preloadedData) {

        List<ResourceAllocation> existingAllocations =
                preloadedData.getAllocationsByResource()
                        .getOrDefault(resourceId, new ArrayList<>());

    /*
     STEP 1 — Monthly override limit
    */

        LocalDate startDate = request.getAllocationStartDate();

        LocalDateTime startOfMonth = startDate
                .withDayOfMonth(1)
                .atStartOfDay();

        LocalDateTime endOfMonth = startDate
                .withDayOfMonth(startDate.lengthOfMonth())
                .atTime(23, 59, 59);

        long monthlyOverrides = allocationModificationRepository.countMonthlyOverrides(
                resourceId,
                startOfMonth,
                endOfMonth
        );

        if (monthlyOverrides >= 3) {

            throw new AllocationExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "OVERRIDE_LIMIT_EXCEEDED",
                    "Resource already used override 3 times this month"
            );
        }

    /*
     STEP 2 — Get internal pool allocation
    */

        ResourceState state = resourceStateRepository
                .findByResourceIdAndCurrentFlagTrue(resourceId)
                .orElseThrow(() -> new AllocationExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "RESOURCE_STATE_MISSING",
                        "Resource " + resourceId + " has no state record. Please initialize resource state first."
                ));

        int internalAllocation = state.getInternalAllocationPercentage() != null
                ? state.getInternalAllocationPercentage()
                : 0;

    /*
     STEP 3 — Calculate current project allocations (ACTIVE + PLANNED)
    */

        int currentProjectAllocation = existingAllocations.stream()
                .filter(a -> a.getAllocationStatus() == AllocationStatus.ACTIVE
                        || a.getAllocationStatus() == AllocationStatus.PLANNED)
                .mapToInt(ResourceAllocation::getAllocationPercentage)
                .sum();

        int requested = request.getAllocationPercentage();

    /*
     STEP 4 — Calculate total allocation (Internal + Project + Requested)
    */

        int totalAllocation = internalAllocation + currentProjectAllocation + requested;

    /*
     STEP 5 — Enforce 130% maximum limit
    */

        if (totalAllocation > 130) {

            throw new AllocationExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "MAX_ALLOCATION_EXCEEDED",
                    String.format("Total allocation would be %d%% (Internal: %d%%, Project: %d%%, Requested: %d%%) which exceeds the maximum allowed limit of 130%%",
                            totalAllocation, internalAllocation, currentProjectAllocation, requested)
            );
        }

    /*
     STEP 6 — Check if special conditions apply for >100% allocation
    */

        if (totalAllocation > 100) {
            // Check if request has approval for beyond capacity allocation
            if (!Boolean.TRUE.equals(request.getRequestBeyondCapacityApproval())) {
                throw new AllocationExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "CAPACITY_APPROVAL_REQUIRED",
                        String.format("Allocation exceeds 100%% (Total would be %d%%). Approval for beyond-capacity allocation required.", totalAllocation)
                );
            }

            // Additional validation for 130% special conditions
            if (totalAllocation > 120) {
                if (request.getOverrideJustification() == null || request.getOverrideJustification().isBlank()) {
                    throw new AllocationExceptionHandler(
                            HttpStatus.BAD_REQUEST,
                            "HIGH_OVERRIDE_JUSTIFICATION_REQUIRED",
                            "Allocation above 120% requires detailed justification for special conditions."
                    );
                }

                // Check duration limit for high allocations
                long overrideDays = ChronoUnit.DAYS.between(
                        request.getAllocationStartDate(),
                        request.getAllocationEndDate()
                ) + 1;

                if (overrideDays > 14) {
                    throw new AllocationExceptionHandler(
                            HttpStatus.BAD_REQUEST,
                            "HIGH_OVERRIDE_DURATION_EXCEEDED",
                            "Allocation above 120% allowed only for maximum 14 days"
                    );
                }
            }
        }

    /*
     STEP 7 — Return override status
    */

        return totalAllocation > 100; // true if override is used
    }

    /**
     * Validates that client and project are active before allocation
     * This validation applies to all allocations regardless of skipValidation flag
     */
    private void validateClientAndProjectStatus(Demand demand, Project project) {
        // Get project from demand if demand exists, otherwise use direct project
        Project targetProject = (demand != null) ? demand.getProject() : project;
        
        if (targetProject == null) {
            throw new AllocationExceptionHandler(
                HttpStatus.BAD_REQUEST,
                "PROJECT_NOT_FOUND",
                "Project not found for allocation"
            );
        }
        
        // Validate project status — only ACTIVE is allowed (APPROVED is a pre-live state)
        if (targetProject.getProjectStatus() != ProjectStatus.ACTIVE) {
            throw new AllocationExceptionHandler(
                HttpStatus.BAD_REQUEST,
                "PROJECT_INACTIVE",
                "Project '" + targetProject.getName() + "' is not active. Current status: " +
                (targetProject.getProjectStatus() != null ? targetProject.getProjectStatus().name() : "UNKNOWN")
            );
        }

        // Validate client status
        if (targetProject.getClient() != null &&
                targetProject.getClient().getStatus() != RecordStatus.ACTIVE) {
            throw new AllocationExceptionHandler(
                HttpStatus.BAD_REQUEST,
                "CLIENT_INACTIVE",
                "Client '" + targetProject.getClient().getClientName() + "' is not active. Current status: " +
                (targetProject.getClient().getStatus() != null ? targetProject.getClient().getStatus().name() : "UNKNOWN")
            );
            }
        }
    }
