package com.service_imple.allocation_service_imple;

import com.dto.allocation_dto.AllocationFailure;
import com.dto.allocation_dto.AllocationRequestDTO;
import com.dto.allocation_dto.AllocationResponseDTO;
import com.dto.allocation_dto.SkillGapAnalysisRequestDTO;
import com.dto.allocation_dto.SkillGapAnalysisResponseDTO;
import com.dto.allocation_dto.ConflictDetectionResult;
import com.dto.allocation_dto.AllocationConflictDTO;
import com.dto.allocation_dto.ConflictResolutionDTO;
import com.dto.ApiResponse;
import com.entity.allocation_entities.ResourceAllocation;
import com.entity.allocation_entities.AllocationConflict;
import com.entity.availability_entities.ResourceAvailabilityLedger;
import com.entity.demand_entities.Demand;
import com.entity.project_entities.Project;
import com.entity.resource_entities.Resource;
import com.entity.skill_entities.*;
import com.entity.client_entities.Client;
import com.entity.resource_entities.Resource;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.centralised_enums.ClientTier;
import com.entity_enums.demand_enums.DemandCommitment;
import com.entity_enums.demand_enums.DemandStatus;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.allocation_repo.AllocationConflictRepository;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.client_repo.ClientRepo;
import com.repo.resource_repo.ResourceRepository;
import com.repo.demand_repo.DemandRepository;
import com.repo.project_repo.ProjectRepository;
import com.repo.availability_repo.ResourceAvailabilityLedgerRepository;
import com.repo.skill_repo.*;
import com.service_interface.allocation_service_interface.AllocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AllocationServiceImple implements AllocationService {

    private final AllocationRepository allocationRepository;
    private final ResourceRepository resourceRepository;
    private final DemandRepository demandRepository;
    private final ProjectRepository projectRepository;
    private final ResourceAvailabilityLedgerRepository ledgerRepository;
    private final ResourceSkillRepository resourceSkillRepository;
    private final ResourceCertificateRepository resourceCertificateRepository;
    private final SkillRepository skillRepository;
    private final ResourceSubSkillRepository resourceSubSkillRepository;
    private final ProficiencyLevelRepository proficiencyLevelRepository;
    private final CertificateRepository certificateRepository;
    private final DeliveryRoleExpectationRepository deliveryRoleExpectationRepository;
    private final MeterRegistry meterRegistry;
    private final AllocationConflictRepository conflictRepository;
    private final ClientRepo clientRepo;

    private void validateResourceCapacity(AllocationRequestDTO request) {
        validateResourceCapacityInternal(null, request);
    }

    private void validateResourceCapacityForUpdate(UUID allocationId, AllocationRequestDTO request) {
        validateResourceCapacityInternal(allocationId, request);
    }

    private void validateResourceCapacityInternal(UUID excludeAllocationId, AllocationRequestDTO request) {
        List<ResourceAllocation> conflictingAllocations =
                allocationRepository.findConflictingAllocationsForResources(
                        request.getResourceId(),
                        request.getAllocationStartDate(),
                        request.getAllocationEndDate()
                );

        int existingAllocation = conflictingAllocations.stream()
                .filter(a -> excludeAllocationId == null || !a.getAllocationId().equals(excludeAllocationId))
                .mapToInt(ResourceAllocation::getAllocationPercentage)
                .sum();

        int newAllocation = request.getAllocationPercentage();
        int total = existingAllocation + newAllocation;

        if (total > 100) {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "OVER_ALLOCATION",
                    (excludeAllocationId == null)
                            ? ("Allocation exceeds resource capacity. Existing: "
                            + existingAllocation + "% , Requested: "
                            + newAllocation + "% , Total: " + total + "%")
                            : "Updating allocation exceeds resource capacity"
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> assignAllocation(AllocationRequestDTO allocationRequest) {

        List<ResourceAllocation> validAllocations = new ArrayList<>();
        List<AllocationFailure> failures = new ArrayList<>();

        try {

            Demand demand = null;
            Project project = null;

            // Validate Demand / Project once
            if (allocationRequest.getDemandId() != null) {

                Optional<Demand> demandOpt = demandRepository.findById(allocationRequest.getDemandId());

                if (demandOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body(
                            new ApiResponse<>(false, "Demand not found", null)
                    );
                }

                demand = demandOpt.get();
                project = demand.getProject();

                if (demand.getDemandCommitment().equals(DemandCommitment.SOFT)) {
                    return ResponseEntity.badRequest().body(
                            new ApiResponse<>(false, "Demand Commitment is SOFT. Allocation not allowed.", null)
                    );
                }
                if(demand.getDemandStatus()!= DemandStatus.APPROVED) {
                    return ResponseEntity.badRequest().body(
                            new ApiResponse<>(false, "Demand Status is not APPROVED. Allocation not allowed.", null)
                    );
                }

            } else if (allocationRequest.getProjectId() != null) {

                Optional<Project> projectOpt = projectRepository.findById(allocationRequest.getProjectId());

                if (projectOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body(
                            new ApiResponse<>(false, "Project not found", null)
                    );
                }

                project = projectOpt.get();
            }




            // 🔹 Process each resource
            for (Long resourceId : allocationRequest.getResourceId()) {

                try {

                    // Step 2: Detect priority conflicts BEFORE creating allocation
                    ConflictDetectionResult conflictResult = detectPriorityConflicts(allocationRequest, resourceId);

                    if (conflictResult.isHasConflicts()) {

                        failures.add(new AllocationFailure(resourceId, "Priority conflict detected: " + conflictResult.getMessage()));
                        continue;
                    }

                    Optional<Resource> resourceOpt = resourceRepository.findById(resourceId);

                    if (resourceOpt.isEmpty()) {
                        failures.add(new AllocationFailure(resourceId, "Resource not found"));
                        continue;
                    }

                    Resource resource = resourceOpt.get();

                    // Validate skills if demand exists
                    if (demand != null) {
                        validateAllocationRequirements(resourceId, demand);
                    }

                    ResourceAllocation allocation = new ResourceAllocation();

                    allocation.setAllocationId(UUID.randomUUID());
                    allocation.setResource(resource);
                    allocation.setDemand(demand);
                    allocation.setProject(project);
                    allocation.setAllocationStartDate(allocationRequest.getAllocationStartDate());
                    allocation.setAllocationEndDate(allocationRequest.getAllocationEndDate());
                    allocation.setAllocationPercentage(allocationRequest.getAllocationPercentage());
                    allocation.setAllocationStatus(allocationRequest.getAllocationStatus());
                    allocation.setCreatedBy(allocationRequest.getCreatedBy());
                    allocation.setCreatedAt(LocalDateTime.now());

                    validAllocations.add(allocation);

                } catch (Exception e) {

                    failures.add(new AllocationFailure(resourceId, e.getMessage()));
                }
            }

            // 🔹 Save valid allocations
            List<ResourceAllocation> savedAllocations = allocationRepository.saveAll(validAllocations);

            for (ResourceAllocation allocation : savedAllocations) {
                updateAvailabilityLedgerForAllocation(allocation);
            }

            int successCount = savedAllocations.size();
            int failureCount = failures.size();

            String message;

            if (failureCount == 0) {
                message = "Allocation Successful";
            } else if (successCount == 0) {
                message = "Allocation Failed";
            } else {
                message = "Allocation Partially Successful";
            }

            Map<String, Object> response = new HashMap<>();
            response.put("successCount", successCount);
            response.put("failureCount", failureCount);
            response.put("savedAllocations", savedAllocations);
            response.put("failedResources", failures);

            return ResponseEntity.ok(
                    new ApiResponse<>(true, message, response)
            );

        } catch (Exception e) {

            return ResponseEntity.internalServerError().body(
                    new ApiResponse<>(false, "Error creating allocation: " + e.getMessage(), null)
            );
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getAllocationById(UUID allocationId) {
        try {
            Optional<ResourceAllocation> allocation = allocationRepository.findById(allocationId);
            AllocationResponseDTO response = mapToResponseDTO(allocation.get());
            if (allocation.isPresent()) {
                return ResponseEntity.ok(
                        new ApiResponse<>(true, "Allocation found", response)
                );
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
                        return ResponseEntity.internalServerError().body(
                new ApiResponse<>(false, "Error retrieving allocation: " + e.getMessage(), null)
            );
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> updateAllocation(UUID allocationId, AllocationRequestDTO allocationRequest) {
        try {
            Optional<ResourceAllocation> existingAllocation = allocationRepository.findById(allocationId);
            
            if (!existingAllocation.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            ResourceAllocation allocation = existingAllocation.get();

            // Update fields
            allocation.setAllocationStartDate(allocationRequest.getAllocationStartDate());
            allocation.setAllocationEndDate(allocationRequest.getAllocationEndDate());
            allocation.setAllocationPercentage(allocationRequest.getAllocationPercentage());
            allocation.setAllocationStatus(allocationRequest.getAllocationStatus());
            validateResourceCapacityForUpdate(allocationId, allocationRequest);

            ResourceAllocation updatedAllocation = allocationRepository.save(allocation);
            
            // Update availability ledger for the modified allocation period
            updateAvailabilityLedgerForAllocation(updatedAllocation);

            AllocationResponseDTO response = mapToResponseDTO(updatedAllocation);
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Allocation updated successfully", response)
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                new ApiResponse<>(false, "Error updating allocation: " + e.getMessage(), null)
            );
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> cancelAllocation(UUID allocationId, String cancelledBy) {
        try {
            Optional<ResourceAllocation> existingAllocation = allocationRepository.findById(allocationId);
            
            if (!existingAllocation.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            ResourceAllocation allocation = existingAllocation.get();
            allocation.setAllocationStatus(AllocationStatus.CANCELLED);
            
            ResourceAllocation cancelledAllocation = allocationRepository.save(allocation);
            
            // Update availability ledger to reflect cancellation
            updateAvailabilityLedgerForAllocation(cancelledAllocation);

            AllocationResponseDTO response = mapToResponseDTO(cancelledAllocation);
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Allocation cancelled successfully", response)
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                new ApiResponse<>(false, "Error cancelling allocation: " + e.getMessage(), null)
            );
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getAllocationsByResource(Long resourceId) {
        try {
            List<ResourceAllocation> allocations = allocationRepository.findByResource_ResourceId(resourceId);
            List<AllocationResponseDTO> response = allocations.stream()
                    .map(this::mapToResponseDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Allocations retrieved successfully", response)
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                new ApiResponse<>(false, "Error retrieving allocations: " + e.getMessage(), null)
            );
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getAllocationsByDemand(UUID demandId) {
        try {
            List<ResourceAllocation> allocations = allocationRepository.findByDemand_DemandId(demandId);
            List<AllocationResponseDTO> response = allocations.stream()
                    .map(this::mapToResponseDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Allocations retrieved successfully", response)
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                new ApiResponse<>(false, "Error retrieving allocations: " + e.getMessage(), null)
            );
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getAllocationsByProject(Long projectId) {
        try {
            List<ResourceAllocation> allocations = allocationRepository.findByProject_PmsProjectId(projectId);
            List<AllocationResponseDTO> response = allocations.stream()
                    .map(this::mapToResponseDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Allocations retrieved successfully", response)
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                new ApiResponse<>(false, "Error retrieving allocations: " + e.getMessage(), null)
            );
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> analyzeSkillGap(SkillGapAnalysisRequestDTO request) {
        try {
            // Validate demand exists
            Demand demand = demandRepository.findById(request.getDemandId())
                .orElseThrow(() -> new ProjectExceptionHandler(
                    HttpStatus.NOT_FOUND,
                    "DEMAND_NOT_FOUND",
                    "Demand not found with ID: " + request.getDemandId()
                ));

            // Validate resource exists
            if (!resourceRepository.existsById(request.getResourceId())) {
                throw new ProjectExceptionHandler(
                    HttpStatus.NOT_FOUND,
                    "RESOURCE_NOT_FOUND",
                    "Resource not found with ID: " + request.getResourceId()
                );
            }

            // Increment cache hit/miss counters
            meterRegistry.counter("skill_gap_analysis_db_queries", "type", "demand_lookup").increment();
            meterRegistry.counter("skill_gap_analysis_db_queries", "type", "resource_exists").increment();

            // Perform comprehensive skill gap analysis
            SkillGapAnalysisResponseDTO analysis = performSkillGapAnalysis(demand, request.getResourceId());

            return ResponseEntity.ok(
                new ApiResponse<>(true, "Skill gap analysis completed successfully", analysis)
            );

        } catch (ProjectExceptionHandler e) {
            log.warn("Skill gap analysis validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, e.getMessage(), null)
            );
        } catch (Exception e) {
            log.error("Skill gap analysis failed unexpectedly: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "Failed to analyze skill gap: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Updates the ResourceAvailabilityLedger for the given allocation.
     * This method recalculates the total allocation percentage for each day
     * in the allocation period and updates the ledger accordingly.
     */
    private void updateAvailabilityLedgerForAllocation(ResourceAllocation allocation) {
        try {
            Long resourceId = allocation.getResource().getResourceId();
            LocalDate startDate = allocation.getAllocationStartDate();
            LocalDate endDate = allocation.getAllocationEndDate();
            
                        
            // For each day in the allocation period, update the ledger
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                updateLedgerForDate(resourceId, currentDate);
                currentDate = currentDate.plusDays(1);
            }
            
                                
        } catch (Exception e) {
                        // Don't throw here to avoid failing the main allocation operation
        }
    }

    /**
     * Updates the ledger for a specific resource and date by calculating
     * the total allocation percentage from all active allocations.
     */
    private void updateLedgerForDate(Long resourceId, LocalDate date) {
        try {
            // Calculate total allocation percentage for this resource on this date
            List<ResourceAllocation> activeAllocations = allocationRepository
                .findActiveAllocationsForResourceOnDate(resourceId, date);
            
            int totalAllocation = activeAllocations.stream()
                .mapToInt(ResourceAllocation::getAllocationPercentage)
                .sum();
            
            // Find existing ledger entry using date range (handles monthly ledgers)
            Optional<ResourceAvailabilityLedger> existingLedger = 
                ledgerRepository.findByResourceIdAndDate(resourceId, date);
            
            ResourceAvailabilityLedger ledger;
            if (existingLedger.isPresent()) {
                ledger = existingLedger.get();
                ledger.setTotalAllocation(totalAllocation);
                ledger.setAvailablePercentage(Math.max(0, 100 - totalAllocation));
                ledger.setAvailabilityTrustFlag(true);
                ledger.setLastCalculatedAt(LocalDateTime.now());
            } else {
                // Create new ledger entry
                ledger = new ResourceAvailabilityLedger();
                ledger.setResource(resourceRepository.findById(resourceId).orElse(null));
                ledger.setPeriodStart(date);
                ledger.setPeriodEnd(date); // Daily ledger
                ledger.setTotalAllocation(totalAllocation);
                ledger.setAvailablePercentage(Math.max(0, 100 - totalAllocation));
                ledger.setAvailabilityTrustFlag(true);
                ledger.setLastCalculatedAt(LocalDateTime.now());
            }
            
            ledgerRepository.save(ledger);
            
        } catch (Exception e) {
                    }
    }

    /**
     * Validates that the resource has all required skills and certificates for the demand
     */
    private void validateAllocationRequirements(Long resourceId, com.entity.demand_entities.Demand demand) {
        LocalDate currentDate = LocalDate.now();
        
        // 🔹 Validate Skills
        if (demand.getRequiredSkills() != null && !demand.getRequiredSkills().isEmpty()) {
            List<ResourceSkill> resourceSkills = resourceSkillRepository
                    .findByResourceIdAndActiveFlagTrue(resourceId);
            
            Set<UUID> resourceSkillIds = resourceSkills.stream()
                    .map(rs -> rs.getSkill().getId())
                    .collect(Collectors.toSet());
            
            Set<UUID> requiredSkillIds = demand.getRequiredSkills().stream()
                    .map(Skill::getId)
                    .collect(Collectors.toSet());
            
            requiredSkillIds.removeAll(resourceSkillIds);
            
            if (!requiredSkillIds.isEmpty()) {
                List<String> missingSkillNames = demand.getRequiredSkills().stream()
                        .filter(skill -> requiredSkillIds.contains(skill.getId()))
                        .map(Skill::getName)
                        .collect(Collectors.toList());
                
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "MISSING_REQUIRED_SKILLS",
                        "Missing required skills: " + String.join(", ", missingSkillNames)
                );
            }
        }
        
        // 🔹 Validate Certificates
        if (demand.getRequiredCertificates() != null && !demand.getRequiredCertificates().isEmpty()) {
            List<ResourceCertificate> resourceCertificates = resourceCertificateRepository
                    .findActiveCertificatesForResource(resourceId, currentDate);
            
            Set<UUID> resourceCertificateIds = resourceCertificates.stream()
                    .map(ResourceCertificate::getCertificateId)
                    .collect(Collectors.toSet());
            
            Set<UUID> requiredCertificateIds = demand.getRequiredCertificates().stream()
                    .map(Certificate::getCertificateId)
                    .collect(Collectors.toSet());
            
            requiredCertificateIds.removeAll(resourceCertificateIds);
            
            if (!requiredCertificateIds.isEmpty()) {
                List<String> missingCertificateNames = demand.getRequiredCertificates().stream()
                        .filter(cert -> requiredCertificateIds.contains(cert.getCertificateId()))
                        .map(cert -> cert.getProviderName() != null ? cert.getProviderName() : cert.getCertificateId().toString())
                        .collect(Collectors.toList());
                
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "MISSING_REQUIRED_CERTIFICATIONS",
                        "Missing required certifications: " + String.join(", ", missingCertificateNames)
                );
            }
        }
    }


    /**
     * Core skill gap analysis implementation
     * Optimized for performance with single queries and in-memory processing
     * Eliminates all repository calls inside loops
     */
    private SkillGapAnalysisResponseDTO performSkillGapAnalysis(Demand demand, Long resourceId) {
        LocalDate currentDate = LocalDate.now();

        log.debug("[PERF] Starting skill gap analysis for demand: {}, resource: {}",
                demand.getDemandId(), resourceId);

        // Single queries to fetch all resource data (performance optimization)
        List<ResourceSkill> resourceSkills = resourceSkillRepository.findByResourceIdAndActiveFlagTrue(resourceId);
        List<ResourceSubSkill> resourceSubSkills = resourceSubSkillRepository.findByResourceIdAndActiveFlagTrue(resourceId);
        List<ResourceCertificate> resourceCertificates = resourceCertificateRepository.findActiveCertificatesForResource(resourceId, currentDate);

        // Count database queries for metrics
        meterRegistry.counter("skill_gap_analysis_db_queries", "type", "resource_skills").increment();
        meterRegistry.counter("skill_gap_analysis_db_queries", "type", "resource_subskills").increment();
        meterRegistry.counter("skill_gap_analysis_db_queries", "type", "resource_certificates").increment();

        // Batch fetch all proficiency levels needed for this analysis
        Map<UUID, ProficiencyLevel> proficiencyLevelMap = getProficiencyLevelMap(resourceSkills, resourceSubSkills);
        meterRegistry.counter("skill_gap_analysis_cache_hit_ratio", "cache", "proficiency_levels").increment();

        // Create lookup maps for O(1) access
        Map<UUID, ResourceSkill> resourceSkillMap = resourceSkills.stream()
            .collect(Collectors.toMap(rs -> rs.getSkill().getId(), rs -> rs));
        Map<UUID, ResourceSubSkill> resourceSubSkillMap = resourceSubSkills.stream()
            .collect(Collectors.toMap(rss -> rss.getSubSkill().getId(), rss -> rss));
        Map<UUID, ResourceCertificate> resourceCertificateMap = resourceCertificates.stream()
            .collect(Collectors.toMap(ResourceCertificate::getCertificateId, rc -> rc));

        // Initialize analysis components
        List<SkillGapAnalysisResponseDTO.SkillComparisonDTO> skillComparisons = new ArrayList<>();
        List<SkillGapAnalysisResponseDTO.CertificateComparisonDTO> certificateComparisons = new ArrayList<>();
        List<SkillGapAnalysisResponseDTO.RecencyWarningDTO> recencyWarnings = new ArrayList<>();

        double totalScore = 0.0;
        int totalRequirements = 0;
        boolean hasMandatoryGap = false;
        boolean hasMandatoryPartial = false;

        // 1. Process DeliveryRoleExpectation (highest priority)
        if (demand.getRole() != null) {
            DeliveryRoleExpectation role = demand.getRole();

            // Fetch ALL role details for this role name with single query
            List<DeliveryRoleExpectation> allRoleExpectations = deliveryRoleExpectationRepository.findByRoleIdWithDetails(role.getRole().getId());

            for (DeliveryRoleExpectation roleExpectation : allRoleExpectations) {
                SkillGapAnalysisResponseDTO.SkillComparisonDTO roleComparison = compareRoleExpectation(
                    roleExpectation, resourceSkillMap, resourceSubSkillMap, proficiencyLevelMap, recencyWarnings
                );

                skillComparisons.add(roleComparison);
                totalScore += roleComparison.getScore();
                totalRequirements++;

                if (roleComparison.getStatus().equals("GAP") && roleExpectation.getMandatoryFlag()) {
                    hasMandatoryGap = true;
                } else if (roleComparison.getStatus().equals("PARTIAL") && roleExpectation.getMandatoryFlag()) {
                    hasMandatoryPartial = true;
                }
            }
        }

        // 2. Process Demand.requiredSkills (excluding those already in role)
        Set<String> roleSkillNames = skillComparisons.stream()
            .filter(sc -> sc.getSubSkillName() == null) // Main skills only
            .map(SkillGapAnalysisResponseDTO.SkillComparisonDTO::getSkillName)
            .collect(Collectors.toSet());

        for (Skill requiredSkill : demand.getRequiredSkills()) {
            if (!roleSkillNames.contains(requiredSkill.getName())) {
                SkillGapAnalysisResponseDTO.SkillComparisonDTO skillComparison = compareRequiredSkill(
                    requiredSkill, resourceSkillMap, recencyWarnings
                );

                skillComparisons.add(skillComparison);
                totalScore += skillComparison.getScore();
                totalRequirements++;
            }
        }

        // 3. Process Demand.requiredCertificates
        for (Certificate requiredCert : demand.getRequiredCertificates()) {
            SkillGapAnalysisResponseDTO.CertificateComparisonDTO certComparison = compareRequiredCertificate(
                requiredCert, resourceCertificateMap
            );

            certificateComparisons.add(certComparison);
            totalScore += certComparison.getScore();
            totalRequirements++;
        }

        // Calculate final metrics
        double matchPercentage = totalRequirements > 0 ? (totalScore / totalRequirements) * 100 : 0.0;
        String riskLevel = calculateOverallRisk(hasMandatoryGap, hasMandatoryPartial, recencyWarnings);
        boolean allocationAllowed = RiskEvaluator.isAllocationAllowed(hasMandatoryGap, hasMandatoryPartial);

        return SkillGapAnalysisResponseDTO.builder()
            .demandId(demand.getDemandId())
            .resourceId(resourceId)
            .matchPercentage(matchPercentage)
            .allocationAllowed(allocationAllowed)
            .riskLevel(riskLevel)
            .skillComparisons(skillComparisons)
            .certificateComparisons(certificateComparisons)
            .recencyWarnings(recencyWarnings)
            .build();
    }

    /**
     * Compares role expectation with resource capabilities
     * Uses pre-fetched proficiency level map to avoid repository calls
     */
    private SkillGapAnalysisResponseDTO.SkillComparisonDTO compareRoleExpectation(
            DeliveryRoleExpectation role, 
            Map<UUID, ResourceSkill> resourceSkillMap,
            Map<UUID, ResourceSubSkill> resourceSubSkillMap,
            Map<UUID, ProficiencyLevel> proficiencyLevelMap,
            List<SkillGapAnalysisResponseDTO.RecencyWarningDTO> recencyWarnings) {

        String skillName = role.getSkill().getName();
        String subSkillName = role.getSubSkill() != null ? role.getSubSkill().getName() : null;
        String requiredProficiency = role.getProficiencyLevel().getProficiencyName();

        // Check for recency warning
        addRecencyWarningIfNeeded(skillName, subSkillName, 
            subSkillName != null ? resourceSubSkillMap.get(role.getSubSkill().getId()) : resourceSkillMap.get(role.getSkill().getId()),
            recencyWarnings);

        if (subSkillName != null) {
            // Sub-skill comparison
            ResourceSubSkill resourceSubSkill = resourceSubSkillMap.get(role.getSubSkill().getId());
            if (resourceSubSkill != null && isActiveAndNotExpired(resourceSubSkill)) {
                ProficiencyLevel resourceProficiency = proficiencyLevelMap.get(resourceSubSkill.getProficiencyId());
                ProficiencyComparator.ProficiencyResult result = ProficiencyComparator.compareProficiency(
                    resourceProficiency, role.getProficiencyLevel());

                return SkillGapAnalysisResponseDTO.SkillComparisonDTO.builder()
                    .skillName(skillName)
                    .subSkillName(subSkillName)
                    .requiredProficiency(requiredProficiency)
                    .resourceProficiency(resourceProficiency != null ? resourceProficiency.getProficiencyName() : null)
                    .mandatory(role.getMandatoryFlag())
                    .status(result.getStatus())
                    .score(result.getScore())
                    .build();
            }
        } else {
            // Main skill comparison
            ResourceSkill resourceSkill = resourceSkillMap.get(role.getSkill().getId());
            if (resourceSkill != null && isActiveAndNotExpired(resourceSkill)) {
                ProficiencyLevel resourceProficiency = proficiencyLevelMap.get(resourceSkill.getProficiencyId());
                ProficiencyComparator.ProficiencyResult result = ProficiencyComparator.compareProficiency(
                    resourceProficiency, role.getProficiencyLevel());

                return SkillGapAnalysisResponseDTO.SkillComparisonDTO.builder()
                    .skillName(skillName)
                    .subSkillName(null)
                    .requiredProficiency(requiredProficiency)
                    .resourceProficiency(resourceProficiency != null ? resourceProficiency.getProficiencyName() : null)
                    .mandatory(role.getMandatoryFlag())
                    .status(result.getStatus())
                    .score(result.getScore())
                    .build();
            }
        }

        // Skill not found or inactive/expired
        return SkillGapAnalysisResponseDTO.SkillComparisonDTO.builder()
            .skillName(skillName)
            .subSkillName(subSkillName)
            .requiredProficiency(requiredProficiency)
            .resourceProficiency(null)
            .mandatory(role.getMandatoryFlag())
            .status("GAP")
            .score(0.0)
            .build();
    }

    /**
     * Compares required skill (compliance skills) with resource capabilities
     */
    private SkillGapAnalysisResponseDTO.SkillComparisonDTO compareRequiredSkill(
            Skill requiredSkill, 
            Map<UUID, ResourceSkill> resourceSkillMap,
            List<SkillGapAnalysisResponseDTO.RecencyWarningDTO> recencyWarnings) {

        ResourceSkill resourceSkill = resourceSkillMap.get(requiredSkill.getId());
        
        // Check for recency warning
        addRecencyWarningIfNeeded(requiredSkill.getName(), null, resourceSkill, recencyWarnings);

        if (resourceSkill != null && isActiveAndNotExpired(resourceSkill)) {
            // Compliance skills don't have proficiency requirements - just existence check
            addRecencyWarningIfNeeded(requiredSkill.getName(), null, resourceSkill, recencyWarnings);
            
            return SkillGapAnalysisResponseDTO.SkillComparisonDTO.builder()
                .skillName(requiredSkill.getName())
                .subSkillName(null)
                .requiredProficiency("N/A")
                .resourceProficiency("Available")
                .mandatory(false) // Compliance skills are not mandatory at this level
                .status("MATCH")
                .score(1.0)
                .build();
        }

        return SkillGapAnalysisResponseDTO.SkillComparisonDTO.builder()
            .skillName(requiredSkill.getName())
            .subSkillName(null)
            .requiredProficiency("N/A")
            .resourceProficiency(null)
            .mandatory(false)
            .status("GAP")
            .score(0.0)
            .build();
    }

    /**
     * Compares required certificate with resource certifications
     */
    private SkillGapAnalysisResponseDTO.CertificateComparisonDTO compareRequiredCertificate(
            Certificate requiredCert, 
            Map<UUID, ResourceCertificate> resourceCertificateMap) {

        ResourceCertificate resourceCert = resourceCertificateMap.get(requiredCert.getCertificateId());

        if (resourceCert != null) {
            return SkillGapAnalysisResponseDTO.CertificateComparisonDTO.builder()
                .certificateName(requiredCert.getProviderName() != null ? requiredCert.getProviderName() : requiredCert.getCertificateId().toString())
                .mandatory(true) // Certificates from compliance are mandatory
                .status("MATCH")
                .score(1.0)
                .build();
        }

        return SkillGapAnalysisResponseDTO.CertificateComparisonDTO.builder()
            .certificateName(requiredCert.getProviderName() != null ? requiredCert.getProviderName() : requiredCert.getCertificateId().toString())
            .mandatory(true)
            .status("GAP")
            .score(0.0)
            .build();
    }

    /**
     * Adds recency warning if skill is old
     */
    private void addRecencyWarningIfNeeded(String skillName, String subSkillName, 
                                        Object resourceSkill, List<SkillGapAnalysisResponseDTO.RecencyWarningDTO> recencyWarnings) {
        LocalDate lastUsedDate = null;
        
        if (resourceSkill instanceof ResourceSkill) {
            lastUsedDate = ((ResourceSkill) resourceSkill).getLastUsedDate();
        } else if (resourceSkill instanceof ResourceSubSkill) {
            lastUsedDate = ((ResourceSubSkill) resourceSkill).getLastUsedDate();
        }

        String riskLevel = RiskEvaluator.evaluateRecencyRisk(lastUsedDate);
        if (!RiskEvaluator.RISK_LOW.equals(riskLevel)) {
            recencyWarnings.add(SkillGapAnalysisResponseDTO.RecencyWarningDTO.builder()
                .skillName(skillName)
                .subSkillName(subSkillName)
                .lastUsedDate(lastUsedDate)
                .riskLevel(riskLevel)
                .yearsUnused(RiskEvaluator.calculateYearsUnused(lastUsedDate))
                .build());
        }
    }

    /**
     * Calculates overall risk level
     */
    private String calculateOverallRisk(boolean hasMandatoryGap, boolean hasMandatoryPartial, 
                                    List<SkillGapAnalysisResponseDTO.RecencyWarningDTO> recencyWarnings) {
        String mandatoryGapRisk = hasMandatoryGap ? RiskEvaluator.RISK_HIGH : RiskEvaluator.RISK_LOW;
        String partialRisk = hasMandatoryPartial ? RiskEvaluator.RISK_HIGH : RiskEvaluator.RISK_LOW;
        
        String recencyRisk = RiskEvaluator.RISK_LOW;
        for (SkillGapAnalysisResponseDTO.RecencyWarningDTO warning : recencyWarnings) {
            if (RiskEvaluator.RISK_HIGH.equals(warning.getRiskLevel())) {
                recencyRisk = RiskEvaluator.RISK_HIGH;
                break;
            } else if (RiskEvaluator.RISK_MEDIUM.equals(warning.getRiskLevel())) {
                recencyRisk = RiskEvaluator.RISK_MEDIUM;
            }
        }

        return RiskEvaluator.aggregateRisk(mandatoryGapRisk, partialRisk, recencyRisk);
    }
    private AllocationResponseDTO mapToResponseDTO(ResourceAllocation allocation) {

        AllocationResponseDTO dto = new AllocationResponseDTO();

        dto.setAllocationId(allocation.getAllocationId());

        if (allocation.getResource() != null) {
            dto.setFullName(allocation.getResource().getFullName());
            dto.setEmail(allocation.getResource().getEmail());
        }

        if (allocation.getDemand() != null) {
            dto.setDemandName(allocation.getDemand().getDemandName());
        }

//        if (allocation.getProject() != null) {
//            dto.setProjectId(allocation.getProject().getPmsProjectId());
//        }

        dto.setAllocationStartDate(allocation.getAllocationStartDate());
        dto.setAllocationEndDate(allocation.getAllocationEndDate());
        dto.setAllocationPercentage(allocation.getAllocationPercentage());
        dto.setAllocationStatus(allocation.getAllocationStatus().name());
        dto.setCreatedBy(allocation.getCreatedBy());

        return dto;
    }

    /**
     * Helper methods
     */
    private DeliveryRoleExpectation fetchFullRoleExpectation(UUID roleId) {
        return deliveryRoleExpectationRepository.findByIdWithDetails(roleId).orElse(null);
    }

    /**
     * Get proficiency level map with caching
     * Caches all active proficiency levels to optimize performance
     */
    @Cacheable(value = "proficiencyLevels", key = "'allActiveProficiencyLevels'")
    private Map<UUID, ProficiencyLevel> getProficiencyLevelMap(List<ResourceSkill> resourceSkills, List<ResourceSubSkill> resourceSubSkills) {
        // Collect all unique proficiency IDs needed
        Set<UUID> proficiencyIds = new HashSet<>();
        resourceSkills.forEach(rs -> proficiencyIds.add(rs.getProficiencyId()));
        resourceSubSkills.forEach(rss -> proficiencyIds.add(rss.getProficiencyId()));
        
        if (proficiencyIds.isEmpty()) {
            return new HashMap<>();
        }
        
        // Fetch all active proficiency levels (cached)
        List<ProficiencyLevel> allActiveProficiencyLevels = proficiencyLevelRepository.findAllActiveProficiencyLevels();
        
        // Filter to only needed proficiency levels and create map
        return allActiveProficiencyLevels.stream()
            .filter(pl -> proficiencyIds.contains(pl.getProficiencyId()))
            .collect(Collectors.toMap(ProficiencyLevel::getProficiencyId, pl -> pl));
    }

    /**
     * Clear proficiency levels cache
     * Call this when proficiency levels are updated
     */
    @CacheEvict(value = "proficiencyLevels", allEntries = true)
    public void clearProficiencyLevelsCache() {
        // Cache cleared - next call will reload from database
    }

    @Override
    public ConflictDetectionResult detectPriorityConflicts(AllocationRequestDTO allocationRequest) {
        try {
            List<ConflictDetectionResult.PriorityConflictDetail> allConflicts = new ArrayList<>();
            List<String> processedResources = new ArrayList<>();
            List<String> failedResources = new ArrayList<>();
            
            // Process each resource in the request
            for (Long resourceId : allocationRequest.getResourceId()) {
                try {
                    // Check if resource exists
                    if (!resourceRepository.existsById(resourceId)) {
                        failedResources.add("Resource " + resourceId + " not found");
                        continue;
                    }
                    
                    // Detect conflicts for this specific resource
                    ConflictDetectionResult resourceConflictResult = detectPriorityConflicts(allocationRequest, resourceId);
                    
                    if (resourceConflictResult.isHasConflicts()) {
                        allConflicts.addAll(resourceConflictResult.getConflicts());
                    }
                    
                    processedResources.add("Resource " + resourceId + " processed");
                    
                } catch (Exception e) {
                    failedResources.add("Resource " + resourceId + " error: " + e.getMessage());
                }
            }
            
            // Determine overall result
            boolean hasConflicts = !allConflicts.isEmpty();
            boolean hasFailures = !failedResources.isEmpty();
            
            // Build comprehensive response
            ConflictDetectionResult.ConflictDetectionResultBuilder resultBuilder = ConflictDetectionResult.builder()
                    .hasConflicts(hasConflicts)
                    .processedResources(processedResources)
                    .failedResources(failedResources);
            
            // Set conflict details if any
            if (hasConflicts) {
                resultBuilder
                    .conflictType("PRIORITY_CONFLICT")
                    .severity("HIGH")
                    .conflicts(allConflicts)
                    .message("Priority conflicts detected for " + allConflicts.size() + " resource(s)");
            }
            
            // Set failure details if any
            if (hasFailures) {
                String failureMessage = "Processing failed for " + failedResources.size() + " resource(s)";
                if (hasConflicts) {
                    resultBuilder.message(resultBuilder.build().getMessage() + "; " + failureMessage);
                } else {
                    resultBuilder
                        .conflictType("PROCESSING_ERROR")
                        .severity("MEDIUM")
                        .message(failureMessage);
                }
            }
            
            // Success case
            if (!hasConflicts && !hasFailures) {
                resultBuilder
                    .message("All resources processed successfully - no conflicts detected")
                    .summary("Successfully validated " + processedResources.size() + " resource(s)");
            }
            
            return resultBuilder.build();
            
        } catch (Exception e) {
            // Edge case: Complete system failure
            return ConflictDetectionResult.builder()
                    .hasConflicts(true)
                    .conflictType("SYSTEM_ERROR")
                    .severity("CRITICAL")
                    .message("System error during conflict detection: " + e.getMessage())
                    .conflicts(java.util.List.of(
                        ConflictDetectionResult.PriorityConflictDetail.builder()
                            .conflictId(java.util.UUID.randomUUID())
                            .conflictType("SYSTEM_ERROR")
                            .severity("CRITICAL")
                            .message("Unable to process allocation request due to system error")
                            .recommendedActions(java.util.List.of(
                                "Please try again later",
                                "Contact system administrator if problem persists",
                                "Verify request format and try again"
                            ))
                            .build()
                    ))
                    .build();
        }
    }

    /**
     * Clear all skill-related caches
     * Call this when any skill reference data is updated
     */
    @CacheEvict(value = {"proficiencyLevels", "skills", "subSkills", "certificates"}, allEntries = true)
    public void clearAllSkillCaches() {
        // All skill-related caches cleared
    }

    private boolean isActiveAndNotExpired(ResourceSkill resourceSkill) {
        return resourceSkill.getActiveFlag();
    }

    private boolean isActiveAndNotExpired(ResourceSubSkill resourceSubSkill) {
        return resourceSubSkill.getActiveFlag();
    }

    // ==================== CONFLICT DETECTION METHODS ====================

    /**
     * Detects priority conflicts for a new allocation request
     */
    public ConflictDetectionResult detectPriorityConflicts(AllocationRequestDTO allocationRequest, Long resourceId) {
        try {
            // Fetch existing allocations for the specified resource
            List<ResourceAllocation> existingAllocations = allocationRepository.findByResource_ResourceId(resourceId);

            // Filter out ended and cancelled allocations
            List<ResourceAllocation> activeAllocations = existingAllocations.stream()
                    .filter(alloc -> alloc.getAllocationStatus() != AllocationStatus.ENDED &&
                                       alloc.getAllocationStatus() != AllocationStatus.CANCELLED)
                    .toList();

            // Check for priority conflicts - since we process one resource at a time,
            // we only need to find the FIRST conflict (no need to collect all)
            for (ResourceAllocation existingAlloc : activeAllocations) {
                if (hasDateOverlap(allocationRequest, existingAlloc)) {
                    // Inline the checkPriorityConflict logic since we only handle one resource
                    PriorityLevel newClientPriority = getClientPriority(allocationRequest);
                    PriorityLevel existingClientPriority = getClientPriority(existingAlloc);
                    
                    AllocationStatus newStatus = allocationRequest.getAllocationStatus();
                    AllocationStatus existingStatus = existingAlloc.getAllocationStatus();
                    
                    ClientTier newTier = mapPriorityLevelToClientTier(newClientPriority);
                    ClientTier existingTier = mapPriorityLevelToClientTier(existingClientPriority);
                    DemandCommitment newCommitment = mapAllocationStatusToDemandCommitment(newStatus);
                    DemandCommitment existingCommitment = mapAllocationStatusToDemandCommitment(existingStatus);
                    
                    if (isPriorityMismatch(existingTier, existingCommitment, newTier, newCommitment)) {
                        // Create conflict detail inline
                        ConflictDetectionResult.AllocationRequestSummary requestSummary =
                            ConflictDetectionResult.AllocationRequestSummary.builder()
                                .demandId(allocationRequest.getDemandId())
                                .projectId(allocationRequest.getProjectId())
                                .resourceId(allocationRequest.getResourceId().get(0))
                                .allocationStatus(newStatus.name())
                                .allocationStartDate(allocationRequest.getAllocationStartDate())
                                .allocationEndDate(allocationRequest.getAllocationEndDate())
                                .allocationPercentage(allocationRequest.getAllocationPercentage())
                                .build();
                        
                        ConflictDetectionResult.PriorityConflictDetail conflict = 
                            ConflictDetectionResult.PriorityConflictDetail.builder()
                                .conflictId(java.util.UUID.randomUUID())
                                .resourceId(allocationRequest.getResourceId().get(0))
                                .resourceName(getResourceName(allocationRequest.getResourceId().get(0)))
                                .existingAllocationId(existingAlloc.getAllocationId())
                                .newAllocationRequest(requestSummary)
                                .conflictType("PRIORITY_CONFLICT")
                                .severity("HIGH")
                                .message(String.format(
                                        "Priority conflict detected: Higher priority client (%s - Tier %s) requesting PLANNED allocation " +
                                        "while lower priority client (%s - Tier %s) has ACTIVE allocation for resource %s",
                                        getClientName(allocationRequest), newClientPriority.getDisplayName(),
                                        getClientName(existingAlloc), existingClientPriority.getDisplayName(),
                                        getResourceName(allocationRequest.getResourceId().get(0)))
                                )
                                .existingClientName(getClientName(existingAlloc))
                                .existingClientTier(existingClientPriority.getDisplayName())
                                .existingAllocationType(existingStatus.name())
                                .newClientName(getClientName(allocationRequest))
                                .newClientTier(newClientPriority.getDisplayName())
                                .newAllocationType(newStatus.name())
                                .recommendedActions(getRecommendedActions())
                                .build();
                        
                        // Return immediately with single conflict since we process one resource at a time
                        return ConflictDetectionResult.builder()
                                .hasConflicts(true)
                                .conflictType("PRIORITY_CONFLICT")
                                .severity("HIGH")
                                .conflicts(java.util.List.of(conflict))
                                .build();
                    }
                }
            }

            // No conflicts found
            return ConflictDetectionResult.builder()
                    .hasConflicts(false)
                    .build();

        } catch (Exception e) {
            log.error("Error detecting priority conflicts for resource {}: {}",
                    resourceId, e.getMessage(), e);
            return ConflictDetectionResult.builder()
                    .hasConflicts(true)
                    .conflictType("DETECTION_ERROR")
                    .severity("HIGH")
                    .message("Error detecting conflicts: " + e.getMessage())
                    .build();
        }
    }

    
    /**
     * Checks if new allocation overlaps with existing allocation dates
     */
    private boolean hasDateOverlap(AllocationRequestDTO newAllocation, ResourceAllocation existingAllocation) {
        LocalDate newStart = newAllocation.getAllocationStartDate();
        LocalDate newEnd = newAllocation.getAllocationEndDate();
        LocalDate existingStart = existingAllocation.getAllocationStartDate();
        LocalDate existingEnd = existingAllocation.getAllocationEndDate();

        return overlaps(newStart, newEnd, existingStart, existingEnd);
    }

    /**
     * Helper methods for conflict detection
     */
    private PriorityLevel getClientPriority(AllocationRequestDTO allocation) {

        if(allocation.getDemandId()!=null){

            Demand demand = demandRepository
                    .findById(allocation.getDemandId())
                    .orElseThrow(() -> new RuntimeException("Demand not found"));

            return demand.getProject()
                    .getClient()
                    .getPriorityLevel();
        }

        if(allocation.getProjectId()!=null){

            Project project = projectRepository
                    .findById(allocation.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            return project.getClient()
                    .getPriorityLevel();
        }

        return PriorityLevel.LOW;
    }

    private PriorityLevel getClientPriority(ResourceAllocation allocation) {
        try {
            Client client = getClientFromAllocation(allocation);
            return client.getPriorityLevel();
        } catch (Exception e) {
            log.warn("Could not get client priority for allocation {}: {}",
                    allocation.getAllocationId(), e.getMessage());
            return PriorityLevel.LOW; // Default to lowest priority
        }
    }

    private Client getClientFromAllocation(ResourceAllocation allocation) {
        if (allocation.getDemand() != null) {
            return allocation.getDemand().getProject().getClient();
        } else if (allocation.getProject() != null) {
            return allocation.getProject().getClient();
        }
        throw new RuntimeException("No client found for allocation: " + allocation.getAllocationId());
    }

    private String getClientName(AllocationRequestDTO allocation) {
        // This would need to be implemented based on your DTO structure
        return "Client Name"; // Placeholder
    }

    private String getClientName(ResourceAllocation allocation) {
        try {
            return getClientFromAllocation(allocation).getClientName();
        } catch (Exception e) {
            return "Unknown Client";
        }
    }

    private String getResourceName(Long resourceId) {
        // This would need to fetch the resource name
        try {
            Resource resource = resourceRepository.findById(resourceId).orElse(null);
            return resource != null ? resource.getFullName() : "Resource " + resourceId;
        } catch (Exception e) {
            return "Resource " + resourceId;
        }
    }

    private List<String> getRecommendedActions() {
        return List.of(
                "Upgrade higher-priority allocation from PLANNED → ACTIVE",
                "Downgrade or cancel lower-priority ACTIVE allocation",
                "Override and keep existing allocation"
        );
    }

    private boolean overlaps(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return !end1.isBefore(start2) && !start1.isAfter(end2);
    }

    private DemandCommitment mapAllocationStatusToDemandCommitment(AllocationStatus status) {
        if (status == null) {
            return DemandCommitment.SOFT;
        }
        switch (status) {
            case ACTIVE:
                return DemandCommitment.CONFIRMED;
            case PLANNED:
                return DemandCommitment.SOFT;
            default:
                return DemandCommitment.CONFIRMED;
        }
    }

    private ClientTier mapPriorityLevelToClientTier(PriorityLevel priorityLevel) {
        switch (priorityLevel) {
            case CRITICAL: return ClientTier.TIER_1_PLATINUM;
            case HIGH: return ClientTier.TIER_2_GOLD;
            case MEDIUM: return ClientTier.TIER_3_SILVER;
            case LOW: return ClientTier.TIER_4_BRONZE;
            default: return ClientTier.TIER_4_BRONZE;
        }
    }

    private boolean isPriorityMismatch(ClientTier lowerTier, DemandCommitment lowerCommitment,
                                       ClientTier higherTier, DemandCommitment higherCommitment) {
        return lowerTier.isLowerPriorityThan(higherTier)
                && lowerCommitment == DemandCommitment.CONFIRMED
                && higherCommitment == DemandCommitment.SOFT;
    }

    // ==================== ALLOCATION CONFLICT METHODS ====================

    /**
     * Detects priority conflicts for a resource's allocations
     */
    public List<AllocationConflictDTO> detectAllocationConflicts(Long resourceId) {
        List<ResourceAllocation> allocations = allocationRepository.findByResource_ResourceId(resourceId);
        List<AllocationConflict> conflicts = new ArrayList<>();

        // Check all pairs of allocations for priority mismatches
        for (int i = 0; i < allocations.size(); i++) {
            for (int j = i + 1; j < allocations.size(); j++) {
                ResourceAllocation alloc1 = allocations.get(i);
                ResourceAllocation alloc2 = allocations.get(j);

                if (hasDateOverlap(alloc1, alloc2)) {
                    AllocationConflict conflict = checkPriorityMismatch(alloc1, alloc2);
                    if (conflict != null) {
                        conflicts.add(conflict);
                    }
                }
            }
        }

        // Save detected conflicts
        List<AllocationConflict> savedConflicts = conflictRepository.saveAll(conflicts);

        // Convert to DTOs and trigger alerts
        return savedConflicts.stream()
                .map(this::convertToDTO)
                .peek(this::triggerConflictAlert)
                .collect(Collectors.toList());
    }

    /**
     * Resolves a conflict with the specified action
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> resolveAllocationConflict(UUID conflictId, ConflictResolutionDTO resolution) {
        try {
            AllocationConflict conflict = conflictRepository.findById(conflictId)
                    .orElseThrow(() -> new RuntimeException("Conflict not found: " + conflictId));

            // Apply resolution action
            applyResolutionAction(conflict, resolution.getResolutionAction());

            // Update conflict status
            conflict.setResolutionStatus("RESOLVED");
            conflict.setResolutionAction(resolution.getResolutionAction());
            conflict.setResolvedBy(resolution.getResolvedBy());
            conflict.setResolvedAt(LocalDateTime.now());
            conflict.setResolutionNotes(resolution.getResolutionNotes());

            conflictRepository.save(conflict);

            log.info("Conflict resolved: {} - Action: {} - By: {}",
                    conflictId, resolution.getResolutionAction(), resolution.getResolvedBy());

            return ResponseEntity.ok(new ApiResponse<>(true, "Conflict resolved successfully", null));

        } catch (Exception e) {
            log.error("Error resolving conflict {}: {}", conflictId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error resolving conflict: " + e.getMessage(), null));
        }
    }

    /**
     * Gets pending conflicts for a resource
     */
    public List<AllocationConflictDTO> getPendingConflictsForResource(Long resourceId) {
        List<AllocationConflict> conflicts = conflictRepository.findByResource_ResourceIdAndResolutionStatus(resourceId, "PENDING");
        return conflicts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Gets all pending conflicts
     */
    public List<AllocationConflictDTO> getAllPendingConflicts() {
        List<AllocationConflict> conflicts = conflictRepository.findAllPendingConflicts();
        return conflicts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Checks if two allocations have priority mismatch
     */
    private AllocationConflict checkPriorityMismatch(ResourceAllocation alloc1, ResourceAllocation alloc2) {
        ClientTier tier1 = getClientTier(alloc1);
        ClientTier tier2 = getClientTier(alloc2);
        DemandCommitment commitment1 = getDemandCommitment(alloc1);
        DemandCommitment commitment2 = getDemandCommitment(alloc2);

        // Check for priority mismatch: lower priority client with hard allocation vs higher priority client with soft allocation
        if (isPriorityMismatch(tier1, commitment1, tier2, commitment2)) {
            return createConflict(alloc1, alloc2, tier1, tier2, commitment1, commitment2);
        } else if (isPriorityMismatch(tier2, commitment2, tier1, commitment1)) {
            return createConflict(alloc2, alloc1, tier2, tier1, commitment2, commitment1);
        }

        return null;
    }

    /**
     * Creates an allocation conflict entity
     */
    private AllocationConflict createConflict(ResourceAllocation lowerPriorityAlloc, ResourceAllocation higherPriorityAlloc,
                                            ClientTier lowerTier, ClientTier higherTier,
                                            DemandCommitment lowerCommitment, DemandCommitment higherCommitment) {

        String severity = determineConflictSeverity(lowerTier, higherTier);
        String recommendation = generateRecommendation(lowerTier, higherTier, lowerCommitment, higherCommitment);

        return AllocationConflict.builder()
                .resource(lowerPriorityAlloc.getResource())
                .lowerPriorityAllocationId(lowerPriorityAlloc.getAllocationId())
                .higherPriorityAllocationId(higherPriorityAlloc.getAllocationId())
                .lowerPriorityClientName(getClientName(lowerPriorityAlloc))
                .higherPriorityClientName(getClientName(higherPriorityAlloc))
                .lowerPriorityClientTier(lowerTier.getDisplayName())
                .higherPriorityClientTier(higherTier.getDisplayName())
                .lowerPriorityAllocationType(lowerCommitment.name())
                .higherPriorityAllocationType(higherCommitment.name())
                .conflictType("PRIORITY_MISMATCH")
                .conflictSeverity(severity)
                .recommendation(recommendation)
                .resolutionStatus("PENDING")
                .build();
    }

    /**
     * Applies the resolution action to allocations
     */
    private void applyResolutionAction(AllocationConflict conflict, String action) {
        switch (action) {
            case "UPGRADE":
                // Upgrade higher priority allocation to confirmed
                upgradeAllocation(conflict.getHigherPriorityAllocationId());
                break;
            case "DISPLACE":
                // Displace lower priority allocation
                displaceAllocation(conflict.getLowerPriorityAllocationId());
                break;
            case "KEEP_CURRENT":
                // Keep current allocation as is
                log.info("Keeping current allocation for conflict: {}", conflict.getConflictId());
                break;
            default:
                throw new IllegalArgumentException("Unknown resolution action: " + action);
        }
    }

    /**
     * Upgrades a soft allocation to confirmed
     */
    private void upgradeAllocation(UUID allocationId) {
        ResourceAllocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new RuntimeException("Allocation not found: " + allocationId));

        // Update demand commitment to CONFIRMED if this is a demand-based allocation
        if (allocation.getDemand() != null) {
            allocation.getDemand().setDemandCommitment(DemandCommitment.CONFIRMED);
        }

        allocationRepository.save(allocation);
        log.info("Upgraded allocation to confirmed: {}", allocationId);
    }

    /**
     * Displaces (cancels) a lower priority allocation
     */
    private void displaceAllocation(UUID allocationId) {
        ResourceAllocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new RuntimeException("Allocation not found: " + allocationId));

        allocation.setAllocationStatus(AllocationStatus.CANCELLED);
        allocationRepository.save(allocation);
        log.info("Displaced lower priority allocation: {}", allocationId);
    }

    /**
     * Helper methods for allocation conflicts
     */
    private boolean hasDateOverlap(ResourceAllocation alloc1, ResourceAllocation alloc2) {
        return overlaps(
                alloc1.getAllocationStartDate(),
                alloc1.getAllocationEndDate(),
                alloc2.getAllocationStartDate(),
                alloc2.getAllocationEndDate()
        );
    }

    private ClientTier getClientTier(ResourceAllocation allocation) {
        Client client = getClientFromAllocation(allocation);
        // For now, map existing PriorityLevel to ClientTier
        return mapPriorityLevelToClientTier(client.getPriorityLevel());
    }

    private ClientTier stringToClientTier(String clientTierStr) {
        if (clientTierStr == null) return null;
        switch (clientTierStr) {
            case "Platinum": return ClientTier.TIER_1_PLATINUM;
            case "Gold": return ClientTier.TIER_2_GOLD;
            case "Silver": return ClientTier.TIER_3_SILVER;
            case "Bronze": return ClientTier.TIER_4_BRONZE;
            default: return ClientTier.TIER_4_BRONZE;
        }
    }

    private DemandCommitment getDemandCommitment(ResourceAllocation allocation) {
        if (allocation.getDemand() != null) {
            return allocation.getDemand().getDemandCommitment();
        }
        // For project-based allocations, assume CONFIRMED
        return DemandCommitment.CONFIRMED;
    }

    private String determineConflictSeverity(ClientTier lowerTier, ClientTier higherTier) {
        int tierDifference = Math.abs(lowerTier.getLevel() - higherTier.getLevel());
        if (tierDifference >= 3) {
            return "HIGH";
        } else if (tierDifference >= 2) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    private String generateRecommendation(ClientTier lowerTier, ClientTier higherTier,
                                       DemandCommitment lowerCommitment, DemandCommitment higherCommitment) {
        return String.format("Consider upgrading the %s client's %s allocation or adjusting the %s client's %s allocation.",
                higherTier.getDisplayName(), higherCommitment.name(),
                lowerTier.getDisplayName(), lowerCommitment.name());
    }

    private AllocationConflictDTO convertToDTO(AllocationConflict conflict) {
        List<AllocationConflictDTO.ResolutionOption> options = generateResolutionOptions(conflict);

        return AllocationConflictDTO.builder()
                .conflictId(conflict.getConflictId())
                .resourceId(conflict.getResource().getResourceId())
                .resourceName(conflict.getResource().getFullName())
                .lowerPriorityAllocation(buildAllocationDetails(conflict.getLowerPriorityAllocationId(),
                        conflict.getLowerPriorityClientName(), conflict.getLowerPriorityClientTier(),
                        conflict.getLowerPriorityAllocationType()))
                .higherPriorityAllocation(buildAllocationDetails(conflict.getHigherPriorityAllocationId(),
                        conflict.getHigherPriorityClientName(), conflict.getHigherPriorityClientTier(),
                        conflict.getHigherPriorityAllocationType()))
                .conflictType(conflict.getConflictType())
                .conflictSeverity(conflict.getConflictSeverity())
                .recommendation(conflict.getRecommendation())
                .resolutionOptions(options)
                .resolutionStatus(conflict.getResolutionStatus())
                .detectedAt(conflict.getDetectedAt())
                .resolvedAt(conflict.getResolvedAt())
                .resolvedBy(conflict.getResolvedBy())
                .resolutionNotes(conflict.getResolutionNotes())
                .build();
    }

    private AllocationConflictDTO.AllocationDetails buildAllocationDetails(UUID allocationId, String clientName,
                                                                         String clientTier, String allocationType) {
        ResourceAllocation allocation = allocationRepository.findById(allocationId).orElse(null);
        if (allocation == null) {
            return null;
        }

        return AllocationConflictDTO.AllocationDetails.builder()
                .allocationId(allocationId)
                .clientName(clientName)
                .clientTier(stringToClientTier(clientTier))
                .allocationType(DemandCommitment.valueOf(allocationType))
                .startDate(allocation.getAllocationStartDate())
                .endDate(allocation.getAllocationEndDate())
                .allocationPercentage(allocation.getAllocationPercentage())
                .projectName(allocation.getProject() != null ? allocation.getProject().getName() : null)
                .demandName(allocation.getDemand() != null ? allocation.getDemand().getDemandName() : null)
                .build();
    }

    private List<AllocationConflictDTO.ResolutionOption> generateResolutionOptions(AllocationConflict conflict) {
        List<AllocationConflictDTO.ResolutionOption> options = new ArrayList<>();

        options.add(AllocationConflictDTO.ResolutionOption.builder()
                .action("UPGRADE")
                .description("Upgrade higher priority allocation to confirmed")
                .impact("Higher priority client gets firm allocation")
                .recommended(true)
                .build());

        options.add(AllocationConflictDTO.ResolutionOption.builder()
                .action("DISPLACE")
                .description("Remove lower priority allocation")
                .impact("Lower priority client loses allocation")
                .recommended(false)
                .build());

        options.add(AllocationConflictDTO.ResolutionOption.builder()
                .action("KEEP_CURRENT")
                .description("Keep current allocations as-is")
                .impact("No change to existing allocations")
                .recommended(false)
                .build());

        return options;
    }

    private void triggerConflictAlert(AllocationConflictDTO conflict) {
        log.error("PRIORITY ALLOCATION CONFLICT DETECTED - Resource: {}, " +
                 "Lower Priority: {} ({}) - {}, Higher Priority: {} ({}) - {}",
                conflict.getResourceName(),
                conflict.getLowerPriorityAllocation().getClientName(),
                conflict.getLowerPriorityAllocation().getClientTier().getDisplayName(),
                conflict.getLowerPriorityAllocation().getAllocationType(),
                conflict.getHigherPriorityAllocation().getClientName(),
                conflict.getHigherPriorityAllocation().getClientTier().getDisplayName(),
                conflict.getHigherPriorityAllocation().getAllocationType());

        log.warn("CONFLICT RECOMMENDATION: {}", conflict.getRecommendation());
    }

    // ==================== INNER CLASSES ====================

}
