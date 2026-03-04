package com.service_imple.allocation_service_imple;

import com.dto.allocation_dto.AllocationRequestDTO;
import com.dto.allocation_dto.SkillGapAnalysisRequestDTO;
import com.dto.allocation_dto.SkillGapAnalysisResponseDTO;
import com.dto.ApiResponse;
import com.entity.allocation_entities.ResourceAllocation;
import com.entity.availability_entities.ResourceAvailabilityLedger;
import com.entity.demand_entities.Demand;
import com.entity.skill_entities.*;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.entity_enums.demand_enums.DemandCommitment;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.allocation_repo.AllocationRepository;
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

    @Override
    public ResponseEntity<ApiResponse<?>> assignAllocation(AllocationRequestDTO allocationRequest) {
        try {
            // Validate resource exists
            if (!resourceRepository.existsById(allocationRequest.getResourceId())) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Resource not found with ID: " + allocationRequest.getResourceId(), null)
                );
            }

            // Validate demand or project exists
            if (allocationRequest.getDemandId() != null) {
                Demand demand =demandRepository.findById(allocationRequest.getDemandId()).get();
                if ( demand == null) {
                    return ResponseEntity.badRequest().body(
                        new ApiResponse<>(false, "Demand not found with ID: " + allocationRequest.getDemandId(), null)
                    );
                }
                if(demand.getDemandCommitment().equals(DemandCommitment.SOFT))
                {
                    return ResponseEntity.badRequest().body(
                        new ApiResponse<>(false, "Demand Commitment is Soft allocation not allowed", null)
                    );
                }
            } else if (allocationRequest.getProjectId() != null) {
                if (!projectRepository.existsById(allocationRequest.getProjectId())) {
                    return ResponseEntity.badRequest().body(
                        new ApiResponse<>(false, "Project not found with ID: " + allocationRequest.getProjectId(), null)
                    );
                }
            }
            // 🔹 Validate overlapping allocation capacity
            validateResourceCapacity(allocationRequest);

            // Create allocation entity
            ResourceAllocation allocation = new ResourceAllocation();
            allocation.setAllocationId(UUID.randomUUID());
            allocation.setResource(resourceRepository.findById(allocationRequest.getResourceId()).orElse(null));
            allocation.setDemand(allocationRequest.getDemandId() != null ? 
                demandRepository.findById(allocationRequest.getDemandId()).orElse(null) : null);
            allocation.setProject(allocationRequest.getProjectId() != null ? 
                projectRepository.findById(allocationRequest.getProjectId()).orElse(null) : null);
            allocation.setAllocationStartDate(allocationRequest.getAllocationStartDate());
            allocation.setAllocationEndDate(allocationRequest.getAllocationEndDate());
            allocation.setAllocationPercentage(allocationRequest.getAllocationPercentage());
            allocation.setAllocationStatus(allocationRequest.getAllocationStatus());
            allocation.setCreatedBy(allocationRequest.getCreatedBy());
            allocation.setCreatedAt(LocalDateTime.now());

            // 🔹 Validate Skills and Certifications if demand exists
            if (allocation.getDemand() != null) {
                validateAllocationRequirements(allocationRequest.getResourceId(), allocation.getDemand());
            }

            ResourceAllocation savedAllocation = allocationRepository.save(allocation);
            
            // Update availability ledger for the allocation period
            updateAvailabilityLedgerForAllocation(savedAllocation);
            
            
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Allocation created successfully", savedAllocation)
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
            
            if (allocation.isPresent()) {
                return ResponseEntity.ok(
                    new ApiResponse<>(true, "Allocation found", allocation.get())
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
            
            
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Allocation updated successfully", updatedAllocation)
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
            
            
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Allocation cancelled successfully", cancelledAllocation)
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
            
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Allocations retrieved successfully", allocations)
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
            
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Allocations retrieved successfully", allocations)
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
            
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Allocations retrieved successfully", allocations)
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

    private void validateResourceCapacity(AllocationRequestDTO request) {

        List<ResourceAllocation> conflictingAllocations =
                allocationRepository.findConflictingAllocations(
                        request.getResourceId(),
                        request.getAllocationStartDate(),
                        request.getAllocationEndDate()
                );

        int existingAllocation = conflictingAllocations.stream()
                .mapToInt(ResourceAllocation::getAllocationPercentage)
                .sum();

        int newAllocation = request.getAllocationPercentage();

        int total = existingAllocation + newAllocation;

        if (total > 100) {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "OVER_ALLOCATION",
                    "Allocation exceeds resource capacity. Existing: "
                            + existingAllocation + "% , Requested: "
                            + newAllocation + "% , Total: " + total + "%"
            );
        }
    }

    private void validateResourceCapacityForUpdate(UUID allocationId, AllocationRequestDTO request) {

        List<ResourceAllocation> conflictingAllocations =
                allocationRepository.findConflictingAllocations(
                        request.getResourceId(),
                        request.getAllocationStartDate(),
                        request.getAllocationEndDate()
                );

        int existingAllocation = conflictingAllocations.stream()
                .filter(a -> !a.getAllocationId().equals(allocationId))
                .mapToInt(ResourceAllocation::getAllocationPercentage)
                .sum();

        int newAllocation = request.getAllocationPercentage();

        if (existingAllocation + newAllocation > 100) {

            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "OVER_ALLOCATION",
                    "Updating allocation exceeds resource capacity"
            );
        }
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
}
