package com.service_imple.bench_service_impl;

import com.dto.bench_dto.BenchResourceDTO;
import com.dto.bench_dto.BenchPoolResponseDTO;
import com.entity.allocation_entities.ResourceAllocation;
import com.entity.bench.ResourceState;
import com.entity.resource_entities.Resource;
import com.entity.skill_entities.ResourceSkill;
import com.entity_enums.bench.BenchReason;
import com.entity_enums.bench.StateType;
import com.entity_enums.bench.SubState;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.bench_repo.BenchDetectionRepository;
import com.repo.skill_repo.ResourceSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bench Detection Service for automatic resource state management
 * 
 * This service maintains RESOURCE_STATE automatically:
 * - Resources with NO active allocation are moved to BENCH
 * - Resources with active allocation are in PROJECT
 * - No manual marking of bench is allowed
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BenchService {

    private final BenchDetectionRepository benchDetectionRepository;
    private final AllocationRepository allocationRepository;
    private final ResourceSkillRepository resourceSkillRepository;

    /**
     * Detect and update bench status for all eligible resources
     * This method should be called whenever:
     * - Allocation created
     * - Allocation updated  
     * - Allocation closed
     * - Role-off executed
     */
    @Transactional
    public void detectBenchResources() {
        log.info("Starting bench detection process");
        
        try {
            // Fetch eligible resourceIds (no active allocation + eligibility criteria)
            List<Long> eligibleResourceIds = benchDetectionRepository.findBenchEligibleResources(LocalDate.now());
            
            log.info("Found {} resources eligible for bench detection", eligibleResourceIds.size());
            
            // Process each eligible resource
            for (Long resourceId : eligibleResourceIds) {
                try {
                    createOrUpdateBenchState(resourceId);
                } catch (Exception e) {
                    log.error("Error processing bench state for resource {}: {}", resourceId, e.getMessage(), e);
                }
            }
            
            log.info("Bench detection process completed successfully");
            
        } catch (Exception e) {
            log.error("Bench detection process failed: {}", e.getMessage(), e);
            throw new RuntimeException("Bench detection failed", e);
        }
    }

    /**
     * Create or update bench state for a specific resource
     * 
     * @param resourceId the resource ID to process
     */
    @Transactional
    public void createOrUpdateBenchState(Long resourceId) {
        log.debug("Processing bench state for resource {}", resourceId);
        
        // Fetch current active RESOURCE_STATE
        Optional<ResourceState> currentState = benchDetectionRepository.findCurrentState(resourceId);
        
        // If already BENCH → do nothing
        if (currentState.isPresent() && currentState.get().getStateType() == StateType.BENCH) {
            log.debug("Resource {} is already in BENCH state, skipping", resourceId);
            return;
        }
        
        // Close old state (set current_flag = false, set effective_to)
        currentState.ifPresent(this::closeCurrentState);
        
        // Insert new BENCH state
        ResourceState newBenchState = createBenchState(resourceId);
        benchDetectionRepository.save(newBenchState);
        
        log.info("Resource {} moved to BENCH state", resourceId);
    }

    /**
     * Move resource to PROJECT state when they get an allocation
     * 
     * @param resourceId the resource ID
     * @param allocationId the allocation ID
     */
    @Transactional
    public void moveToProject(Long resourceId, UUID allocationId) {
        log.debug("Moving resource {} to PROJECT state with allocation {}", resourceId, allocationId);
        
        // Close current BENCH state if exists
        Optional<ResourceState> currentBenchState = benchDetectionRepository.findCurrentBenchState(resourceId);
        currentBenchState.ifPresent(this::closeCurrentState);
        
        // Insert new PROJECT state
        ResourceState newProjectState = createProjectState(resourceId, allocationId);
        benchDetectionRepository.save(newProjectState);
        
        log.info("Resource {} moved to PROJECT state with allocation {}", resourceId, allocationId);
    }

    /**
     * Get all bench resources for frontend display
     */
    @Transactional(readOnly = true)
    public List<BenchResourceDTO> getAllBenchResources() {
        log.debug("Fetching all bench resources for frontend");
        
        List<Resource> benchResources = benchDetectionRepository.findAllBenchResources();
        
        return benchResources.stream()
                .map(this::convertToBenchResourceDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get bench resources by sub-state
     */
    @Transactional(readOnly = true)
    public List<BenchResourceDTO> getBenchResourcesBySubState(SubState subState) {
        log.debug("Fetching bench resources by sub-state: {}", subState);
        
        List<Resource> benchResources = benchDetectionRepository.findBenchResourcesBySubState(subState);
        
        return benchResources.stream()
                .map(this::convertToBenchResourceDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get bench resources by bench reason
     */
    @Transactional(readOnly = true)
    public List<BenchResourceDTO> getBenchResourcesByReason(BenchReason benchReason) {
        log.debug("Fetching bench resources by reason: {}", benchReason);
        
        List<Resource> benchResources = benchDetectionRepository.findBenchResourcesByReason(benchReason);
        
        return benchResources.stream()
                .map(this::convertToBenchResourceDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get bench resources by skill group
     */
    @Transactional(readOnly = true)
    public List<BenchResourceDTO> getBenchResourcesBySkillGroup(String skillGroup) {
        log.debug("Fetching bench resources by skill group: {}", skillGroup);
        
        List<Resource> benchResources = benchDetectionRepository.findBenchResourcesBySkillGroup(skillGroup);
        
        return benchResources.stream()
                .map(this::convertToBenchResourceDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get bench statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getBenchStatistics() {
        log.debug("Fetching bench statistics");
        
        long totalCount = benchDetectionRepository.getTotalBenchCount();
        
        List<Object[]> countBySubState = benchDetectionRepository.getBenchCountBySubState();
        Map<String, Long> subStateCounts = countBySubState.stream()
                .collect(Collectors.toMap(
                        arr -> arr[0].toString(),
                        arr -> (Long) arr[1]
                ));
        
        List<Object[]> countByReason = benchDetectionRepository.getBenchCountByReason();
        Map<String, Long> reasonCounts = countByReason.stream()
                .collect(Collectors.toMap(
                        arr -> arr[0] != null ? arr[0].toString() : "UNKNOWN",
                        arr -> (Long) arr[1]
                ));
        
        return Map.of(
                "totalCount", totalCount,
                "countBySubState", subStateCounts,
                "countByReason", reasonCounts
        );
    }

    /**
     * Convert Resource entity to BenchResourceDTO
     */
    private BenchResourceDTO convertToBenchResourceDTO(Resource resource) {
        // Get the bench state for this resource
        Optional<ResourceState> benchState = benchDetectionRepository.findCurrentBenchState(resource.getResourceId());
        
        if (benchState.isEmpty()) {
            log.warn("Resource {} is in bench resources list but no bench state found", resource.getResourceId());
            return null;
        }
        
        ResourceState state = benchState.get();
        long benchDays = ChronoUnit.DAYS.between(state.getBenchStartDate(), LocalDate.now());
        
        return BenchResourceDTO.builder()
                .resourceId(resource.getResourceId())
                .resourceName(resource.getFullName())
                .email(resource.getEmail())
                .designation(resource.getDesignation())
                .primarySkillGroup(resource.getPrimarySkillGroup())
                .experience(resource.getExperiance())
                .workingLocation(resource.getWorkingLocation())
                .employmentType(resource.getEmploymentType().toString())
                .workforceCategory(resource.getWorkforceCategory().toString())
                .benchStartDate(state.getBenchStartDate())
                .benchReason(state.getBenchReason())
                .subState(state.getSubState())
                .benchDays(benchDays)
                .grade(resource.getGrade())
                .vendorName(resource.getVendorName())
                .dateOfJoining(resource.getDateOfJoining())
                .hourlyCostRate(resource.getHourlyCostRate())
                .currencyType(resource.getCurrencyType())
                .build();
    }

    /**
     * Close current state by setting currentFlag to false and effectiveTo to today
     * 
     * @param resourceState the state to close
     */
    private void closeCurrentState(ResourceState resourceState) {
        resourceState.setCurrentFlag(false);
        resourceState.setEffectiveTo(LocalDate.now());
        benchDetectionRepository.save(resourceState);
        log.debug("Closed state {} for resource {}", resourceState.getStateType(), resourceState.getResourceId());
    }

    /**
     * Create new BENCH state with default values
     * 
     * @param resourceId the resource ID
     * @return new ResourceState entity
     */
    private ResourceState createBenchState(Long resourceId) {
        return ResourceState.builder()
                .resourceId(resourceId)
                .stateType(StateType.BENCH)
                .subState(SubState.READY)
                .benchStartDate(LocalDate.now())
                .benchReason(BenchReason.WAITING_ALLOCATION)
                .effectiveFrom(LocalDate.now())
                .currentFlag(true)
                .createdBy("BENCH_DETECTION_ENGINE")
                .build();
    }

    /**
     * Create new PROJECT state for allocated resource
     * 
     * @param resourceId the resource ID
     * @param allocationId the allocation ID
     * @return new ResourceState entity
     */
    private ResourceState createProjectState(Long resourceId, UUID allocationId) {
        return ResourceState.builder()
                .resourceId(resourceId)
                .stateType(StateType.PROJECT)
                .allocationId(allocationId)
                .effectiveFrom(LocalDate.now())
                .currentFlag(true)
                .createdBy("BENCH_DETECTION_ENGINE")
                .build();
    }

    /**
     * Validate resource state consistency
     * 
     * @param resourceId the resource ID to validate
     * @return true if state is consistent
     */
    @Transactional(readOnly = true)
    public boolean validateResourceState(Long resourceId) {
        // Check if resource has active allocations
        boolean hasActiveAllocations = benchDetectionRepository.hasActiveAllocations(resourceId);
        
        // Check current state
        Optional<ResourceState> currentState = benchDetectionRepository.findCurrentState(resourceId);
        
        if (hasActiveAllocations) {
            // Should be in PROJECT state
            return currentState.isPresent() && currentState.get().getStateType() == StateType.PROJECT;
        } else {
            // Should be in BENCH state
            return currentState.isPresent() && currentState.get().getStateType() == StateType.BENCH;
        }
    }

    /**
     * Get bench statistics
     * 
     * @return number of resources currently in bench
     */
    @Transactional(readOnly = true)
    public long getBenchResourceCount() {
        return benchDetectionRepository.findResourcesInBench().size();
    }

    /**
     * Initialize resource state for newly created resources
     * This method should be called after a resource is successfully saved
     * 
     * @param resourceId the resource ID to initialize state for
     */
    @Transactional
    public void initializeResourceState(Long resourceId) {
        log.info("Initializing resource state for new resource: {}", resourceId);
        
        try {
            // Check if resource already has a current state (prevent duplicates)
            Optional<ResourceState> existingState = benchDetectionRepository.findByResourceIdAndCurrentFlagTrue(resourceId);
            if (existingState.isPresent()) {
                log.warn("Resource {} already has a current state, skipping initialization", resourceId);
                return;
            }

            // Check if resource has active allocation
            Optional<ResourceAllocation> activeAllocation = allocationRepository.findActiveByResourceId(resourceId);
            
            if (activeAllocation.isPresent()) {
                // Resource has active allocation → create PROJECT state
                log.info("Resource {} has active allocation {}, creating PROJECT state", 
                        resourceId, activeAllocation.get().getAllocationId());
                
                ResourceState projectState = createProjectState(resourceId, activeAllocation.get().getAllocationId());
                benchDetectionRepository.save(projectState);
                
            } else {
                // Resource has no allocation → create BENCH state (TRAINING)
                log.info("Resource {} has no active allocation, creating BENCH state", resourceId);
                
                ResourceState benchState = createTrainingBenchState(resourceId);
                benchDetectionRepository.save(benchState);
            }
            
            log.info("Resource state initialization completed for resource: {}", resourceId);
            
        } catch (Exception e) {
            log.error("Error initializing resource state for resource {}: {}", resourceId, e.getMessage(), e);
            throw new RuntimeException("Failed to initialize resource state for resource: " + resourceId, e);
        }
    }

    /**
     * Create new BENCH state with TRAINING sub-state for new resources
     * 
     * @param resourceId the resource ID
     * @return new ResourceState entity
     */
    private ResourceState createTrainingBenchState(Long resourceId) {
        return ResourceState.builder()
                .resourceId(resourceId)
                .stateType(StateType.BENCH)
                .subState(SubState.TRAINING)
                .benchStartDate(LocalDate.now())
                .benchReason(BenchReason.WAITING_ALLOCATION)
                .effectiveFrom(LocalDate.now())
                .currentFlag(true)
                .createdBy("RESOURCE_ONBOARDING")
                .build();
    }

    /**
     * Get bench resources for bench endpoint
     */
    @Transactional(readOnly = true)
    public List<BenchPoolResponseDTO> getBenchResources() {
        log.debug("Fetching bench resources for bench endpoint");
        
        List<Object[]> benchResourcesData = benchDetectionRepository.findBenchResourcesWithDetails();
        List<Long> resourceIds = benchResourcesData.stream()
                .map(arr -> ((Resource) arr[0]).getResourceId())
                .collect(Collectors.toList());
        
        // Get skill details for all resources
        List<Object[]> skillDetails = resourceSkillRepository.findResourceIdAndSkillDetails(resourceIds);
        Map<Long, List<Map<String, String>>> skillsMap = groupSkillsByResource(skillDetails);
        
        return benchResourcesData.stream()
                .map(arr -> convertToBenchPoolResponseDTO((Resource) arr[0], (LocalDate) arr[1], skillsMap))
                .collect(Collectors.toList());
    }

    /**
     * Get pool resources for pool endpoint
     */
    @Transactional(readOnly = true)
    public List<BenchPoolResponseDTO> getPoolResources() {
        log.debug("Fetching pool resources for pool endpoint");
        
        List<Object[]> poolResourcesData = benchDetectionRepository.findPoolResourcesWithDetails();
        List<Long> resourceIds = poolResourcesData.stream()
                .map(arr -> ((Resource) arr[0]).getResourceId())
                .collect(Collectors.toList());
        
        // Get skill details for all resources
        List<Object[]> skillDetails = resourceSkillRepository.findResourceIdAndSkillDetails(resourceIds);
        Map<Long, List<Map<String, String>>> skillsMap = groupSkillsByResource(skillDetails);
        
        return poolResourcesData.stream()
                .map(arr -> convertToBenchPoolResponseDTO((Resource) arr[0], (LocalDate) arr[1], skillsMap))
                .collect(Collectors.toList());
    }

    /**
     * Group skills by resource and format as skill->subskill:proficiency map
     */
    private Map<Long, List<Map<String, String>>> groupSkillsByResource(List<Object[]> skillDetails) {
        Map<Long, List<Map<String, String>>> skillsMap = new HashMap<>();
        
        for (Object[] detail : skillDetails) {
            Long resourceId = (Long) detail[0];
            String skillName = (String) detail[1];
            String proficiency = (String) detail[2];
            
            skillsMap.computeIfAbsent(resourceId, k -> new ArrayList<>())
                    .add(Map.of(skillName, proficiency));
        }
        
        return skillsMap;
    }

    /**
     * Convert Resource entity to BenchPoolResponseDTO
     */
    private BenchPoolResponseDTO convertToBenchPoolResponseDTO(Resource resource, LocalDate benchStartDate, 
                                                               Map<Long, List<Map<String, String>>> skillsMap) {
        // Calculate aging (days in bench/pool)
        long agingDays = ChronoUnit.DAYS.between(benchStartDate, LocalDate.now());
        
        // Calculate cost per day from annual CTC
        double costPerDay = 0.0;
        if (resource.getAnnualCtc() != null) {
            costPerDay = resource.getAnnualCtc().doubleValue() / 365.0;
        }
        
        // Get skills for this resource
        List<Map<String, String>> skillGroups = skillsMap.getOrDefault(resource.getResourceId(), new ArrayList<>());
        
        return BenchPoolResponseDTO.builder()
                .employeeId(resource.getResourceId())
                .resourceName(resource.getFullName())
                .designation(resource.getDesignation())
                .skillGroups(skillGroups)
                .subState(SubState.READY) // Default sub-state
                .allocation(0) // Default allocation count
                .aging((int) agingDays)
                .costPerDay(costPerDay)
                .build();
    }
}
