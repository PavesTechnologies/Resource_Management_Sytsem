package com.service_imple.availability_service_impl;

import com.dto.availability_dto.ResourceTimelineDTO;
import com.dto.availability_dto.ResourceTimelineResponseDTO;
import com.dto.availability_dto.ResourceTimelineApiResponse;
import com.dto.skill_dto.SkillInfoDTO;
import com.dto.skill_dto.CertificationInfoDTO;
import com.dto.allocation_dto.AllocationTimelineItem;
import com.dto.allocation_dto.NoticeInfoDTO;
import com.service_interface.availability_service_interface.ResourceTimelineProjection;
import com.service_interface.availability_service_interface.TimelineKpiProjection;
import com.service_interface.availability_service_interface.AllocationTimelineProjection;
import com.service_interface.availability_service_interface.CurrentProjectProjection;
import com.service_interface.availability_service_interface.CurrentAllocationProjection;
import com.repo.availability_repo.ResourceTimelineRepository;
import com.repo.skill_repo.ResourceSkillRepository;
import com.repo.skill_repo.ResourceSubSkillRepository;
import com.repo.skill_repo.ResourceCertificateRepository;
import com.service_interface.availability_service_interface.ResourceTimelineService;
import com.service_imple.external_api_impl.ExternalApiTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceTimelineServiceImpl implements ResourceTimelineService {

    private final ResourceTimelineRepository resourceTimelineRepository;
    private final ResourceSkillRepository resourceSkillRepository;
    private final ResourceSubSkillRepository resourceSubSkillRepository;
    private final ResourceCertificateRepository resourceCertificateRepository;
    private final ExternalApiTokenService ExternalApiTokenService;

    @Override
    public List<ResourceTimelineDTO> getAllResourceTimelines() {
        // Get all resources without date filtering - use full history mode with default parameters
        List<ResourceTimelineProjection> allResources = resourceTimelineRepository.getResourceTimelineFullHistory(
                null, null, null, null, null, null, null, null, Integer.MAX_VALUE, 0, null);
        
        return allResources.stream()
                .map(this::convertToResourceTimelineDTO)
                .collect(Collectors.toList());
    }
    
    private ResourceTimelineDTO convertToResourceTimelineDTO(ResourceTimelineProjection projection) {
        return ResourceTimelineDTO.builder()
                .id(projection.getId().toString())
                .name(projection.getFullName())
                .avatar(generateAvatarFromName(projection.getFullName()))
                .role(projection.getDesignation())
                .skills(List.of()) // Empty for basic timeline - skills loaded in detailed view
                .location(projection.getWorkingLocation())
                .experience(projection.getExperiance())
                .currentAllocation(null) // Not calculated in basic timeline
                .availableFrom(null) // Not calculated in basic timeline
                .currentProject(List.of()) // Not calculated in basic timeline
                .nextAssignment(null) // Not calculated in basic timeline
                .employmentType(projection.getEmploymentType())
                .utilizationHistory(List.of()) // Not calculated in basic timeline
                .allocationTimeline(List.of()) // Not calculated in basic timeline
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "resource-timelines", key = "#startDate + '-' + #endDate + '-' + #designation + '-' + #location + '-' + #minExp + '-' + #maxExp + '-' + #employmentType + '-' + #status + '-' + #search + '-' + #allocationPercentage + '-' + #project + '-' + #page + '-' + #size")
    public ResourceTimelineApiResponse getResourceTimelineWindow(
            LocalDate startDate,
            LocalDate endDate,
            String designation,
            String location,
            Integer minExp,
            Integer maxExp,
            String employmentType,
            String status,
            String search,
            Integer allocationPercentage,
            String project,
            Integer page,
            Integer size) {
        
        // Validate date requirements
        if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
            return ResourceTimelineApiResponse.error("Both startDate and endDate must be provided together, or both must be null");
        }
        
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            return ResourceTimelineApiResponse.error("startDate must not be after endDate");
        }

        if (search != null && search.isBlank()) {
            search = null;
        }
        
        // Validate pagination
        if (page == null || page < 0) {
            page = 0;
        }
        if (size == null || size < 1 || size > 100) {
            size = 20;
        }
        
        // Get filtered data with pagination
        List<ResourceTimelineProjection> filteredData;
        Long totalCount;
        
        if (startDate != null && endDate != null) {
            // Window Mode - use date-specific queries
            filteredData = resourceTimelineRepository.getResourceTimelineWindow(
                    startDate, endDate, designation, location, employmentType, minExp, maxExp, search, allocationPercentage, project, size, page * size, status);
            
            totalCount = resourceTimelineRepository.getResourceTimelineWindowCount(
                    startDate, endDate, designation, location, employmentType, minExp, maxExp, allocationPercentage, project, status);
        } else {
            // Full History Mode - use date-agnostic queries
            filteredData = resourceTimelineRepository.getResourceTimelineFullHistory(
                    designation, location, employmentType, minExp, maxExp, search, allocationPercentage, project, size, page * size, status);
            
            totalCount = resourceTimelineRepository.getResourceTimelineFullHistoryCount(
                    designation, location, employmentType, minExp, maxExp, allocationPercentage, project, status);
        }
        
        // Fetch allocation timeline, current projects, current allocations, skills, and certifications for all resources
        final Map<Long, List<AllocationTimelineItem>> allocationTimelineMap;
        final Map<Long, List<String>> currentProjectMap;
        final Map<Long, Integer> currentAllocationMap;
        final Map<Long, List<String>> resourceSkillsMap;
        final Map<Long, List<String>> resourceSubSkillsMap;
        final Map<Long, List<SkillInfoDTO>> resourceSkillDetailsMap;
        final Map<Long, List<SkillInfoDTO>> resourceSubSkillDetailsMap;
        final Map<Long, List<CertificationInfoDTO>> resourceCertificationsMap;
        
        if (!filteredData.isEmpty()) {
            List<Long> resourceIds = filteredData.stream()
                    .map(ResourceTimelineProjection::getId)
                    .distinct()
                    .collect(Collectors.toList());
            
            // Fetch allocation timeline
            allocationTimelineMap = resourceTimelineRepository.getAllocationTimeline(resourceIds).stream()
                    .collect(Collectors.groupingBy(
                            AllocationTimelineProjection::getResourceId,
                            Collectors.mapping(this::mapToAllocationTimelineItem, Collectors.toList())
                    ));
            
            // Fetch current projects
            currentProjectMap = resourceTimelineRepository.getCurrentProjects(resourceIds).stream()
                    .collect(Collectors.groupingBy(
                            CurrentProjectProjection::getResourceId,
                            Collectors.mapping(CurrentProjectProjection::getProjectName, Collectors.toList())
                    ));
            
            // Fetch current allocations
            currentAllocationMap = resourceTimelineRepository.getCurrentAllocations(resourceIds).stream()
                    .collect(Collectors.toMap(
                            CurrentAllocationProjection::getResourceId,
                            CurrentAllocationProjection::getCurrentAllocation
                    ));
            
            // Fetch resource skills
            resourceSkillsMap = resourceSkillRepository.findResourceIdAndSkillNames(resourceIds).stream()
                    .collect(Collectors.groupingBy(
                            result -> (Long) result[0], // resource ID is first element
                            Collectors.mapping(result -> (String) result[1], Collectors.toList()) // skill name is second element
                    ));
            
            // Fetch resource sub-skills
            resourceSubSkillsMap = resourceSubSkillRepository.findResourceIdAndSubSkillNames(resourceIds).stream()
                    .collect(Collectors.groupingBy(
                            result -> (Long) result[0], // resource ID is first element
                            Collectors.mapping(result -> (String) result[1], Collectors.toList()) // sub-skill name is second element
                    ));
            
            // Fetch resource skill details with proficiency and last used date
            resourceSkillDetailsMap = resourceSkillRepository.findResourceIdAndSkillDetails(resourceIds).stream()
                    .collect(Collectors.groupingBy(
                            result -> (Long) result[0], // resource ID is first element
                            Collectors.mapping(result -> SkillInfoDTO.from(
                                    (String) result[1], // skill name
                                    (String) result[2], // proficiency name
                                    (java.time.LocalDate) result[3] // last used date
                            ), Collectors.toList())
                    ));
            
            // Fetch resource sub-skill details with proficiency and last used date
            resourceSubSkillDetailsMap = resourceSubSkillRepository.findResourceIdAndSubSkillDetails(resourceIds).stream()
                    .collect(Collectors.groupingBy(
                            result -> (Long) result[0], // resource ID is first element
                            Collectors.mapping(result -> SkillInfoDTO.from(
                                    (String) result[1], // sub-skill name
                                    (String) result[2], // proficiency name
                                    (java.time.LocalDate) result[3] // last used date
                            ), Collectors.toList())
                    ));
            
            // Fetch resource certifications (only active and non-expired)
            LocalDate currentDate = LocalDate.now();
            resourceCertificationsMap = resourceCertificateRepository.findResourceIdAndCertificateDetails(resourceIds, currentDate).stream()
                    .collect(Collectors.groupingBy(
                            result -> (Long) result[0], // resource ID is first element
                            Collectors.mapping(result -> CertificationInfoDTO.builder()
                                    .certificateName((String) result[1]) // certificate name from skill
                                    .providerName((String) result[2]) // provider name
                                    .expiryDate((LocalDate) result[3]) // expiry date
                                    .isActive(true) // active because of query filter
                                    .build(), 
                            Collectors.toList())
                    ));
        } else {
            allocationTimelineMap = Map.of();
            currentProjectMap = Map.of();
            currentAllocationMap = Map.of();
            resourceSkillsMap = Map.of();
            resourceSubSkillsMap = Map.of();
            resourceSkillDetailsMap = Map.of();
            resourceSubSkillDetailsMap = Map.of();
            resourceCertificationsMap = Map.of();
        }
        
        // Map to full response DTOs with status filtering and notice period logic
        List<ResourceTimelineResponseDTO> responseDTOs = filteredData.stream()
                .map(projection -> mapToFullResponseDTO(projection, allocationTimelineMap, currentProjectMap, currentAllocationMap, resourceSkillsMap, resourceSubSkillsMap, resourceSkillDetailsMap, resourceSubSkillDetailsMap, resourceCertificationsMap))
                .filter(dto -> {
                    if (status == null) return true;
                    
                    // Apply notice period throttling to availability calculation
                    Integer effectiveAllocation = applyNoticePeriodThrottling(dto);
                    
                    return switch (status) {
                        case "available" -> effectiveAllocation <= 20;
                        case "partial" -> effectiveAllocation > 20 && effectiveAllocation <= 70;
                        case "allocated" -> effectiveAllocation > 70;
                        default -> true;
                    };
                })
                .collect(Collectors.toList());
        
        return ResourceTimelineApiResponse.success("Timeline fetched successfully", responseDTOs, page, size, totalCount);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceTimelineResponseDTO.ResourceTimelineKpi getTimelineKPI(
            LocalDate startDate, 
            LocalDate endDate,
            String designation,
            String location,
            Integer minExp,
            Integer maxExp,
            String employmentType,
            String status) {
        
        List<TimelineKpiProjection> kpiData;
        
        if (startDate != null && endDate != null) {
            // Window Mode KPI
            kpiData = resourceTimelineRepository.getTimelineKPIWindow(
                    startDate, endDate, designation, location, employmentType, minExp, maxExp, status);
        } else {
            // Overall KPI
            kpiData = resourceTimelineRepository.getTimelineKPIFullHistory(
                    designation, location, employmentType, minExp, maxExp, status);
        }
        
        if (kpiData.isEmpty()) {
            return ResourceTimelineResponseDTO.ResourceTimelineKpi.builder()
                    .totalResources(0L)
                    .fullyAvailable(0L)
                    .partiallyAvailable(0L)
                    .fullyAllocated(0L)
                    .overAllocated(0L)
                    .noticePeriodResources(0L)
                    .availableNoticePeriodResources(0L)
                    .build();
        }
        
        TimelineKpiProjection kpi = kpiData.get(0);
        return ResourceTimelineResponseDTO.ResourceTimelineKpi.builder()
                .totalResources(kpi.getTotalResources())
                .fullyAvailable(kpi.getFullyAvailable())
                .partiallyAvailable(kpi.getPartiallyAvailable())
                .fullyAllocated(kpi.getFullyAllocated())
                .overAllocated(kpi.getOverAllocated())
                .noticePeriodResources(kpi.getNoticePeriodResources() != null ? kpi.getNoticePeriodResources() : 0L)
                .availableNoticePeriodResources(kpi.getAvailableNoticePeriodResources() != null ? kpi.getAvailableNoticePeriodResources() : 0L)
                .build();
    }

    private ResourceTimelineResponseDTO mapToFullResponseDTO(
            ResourceTimelineProjection projection,
            Map<Long, List<AllocationTimelineItem>> allocationTimelineMap,
            Map<Long, List<String>> currentProjectMap,
            Map<Long, Integer> currentAllocationMap,
            Map<Long, List<String>> resourceSkillsMap,
            Map<Long, List<String>> resourceSubSkillsMap,
            Map<Long, List<SkillInfoDTO>> resourceSkillDetailsMap,
            Map<Long, List<SkillInfoDTO>> resourceSubSkillDetailsMap,
            Map<Long, List<CertificationInfoDTO>> resourceCertificationsMap) {
        
        List<AllocationTimelineItem> allocationTimeline = allocationTimelineMap.getOrDefault(
                projection.getId(), List.of());
        
        List<String> currentProjects = currentProjectMap.getOrDefault(
                projection.getId(), List.of());
        
        // Use current allocation from today-based calculation, fallback to 0 if not found
        Integer currentAllocation = currentAllocationMap.getOrDefault(projection.getId(), 0);
        
        // Get skills and sub-skills for this resource
        List<String> skills = resourceSkillsMap.getOrDefault(projection.getId(), List.of());
        List<String> subSkills = resourceSubSkillsMap.getOrDefault(projection.getId(), List.of());
        
        // Get skill details for this resource
        List<SkillInfoDTO> skillDetails = resourceSkillDetailsMap.getOrDefault(projection.getId(), List.of());
        List<SkillInfoDTO> subSkillDetails = resourceSubSkillDetailsMap.getOrDefault(projection.getId(), List.of());
        
        // Combine skills and sub-skills for display
        List<String> allSkills = new ArrayList<>(skills);
        allSkills.addAll(subSkills);
        
        // Combine skill details and sub-skill details
        List<SkillInfoDTO> allSkillDetails = new ArrayList<>(skillDetails);
        allSkillDetails.addAll(subSkillDetails);
        
        // Get certifications for this resource
        List<CertificationInfoDTO> certifications = resourceCertificationsMap.getOrDefault(projection.getId(), List.of());
        
        // Calculate availableFrom date (day after current allocations end)
        LocalDate availableFrom = null;
        if (currentAllocation > 0 && !allocationTimeline.isEmpty()) {
            LocalDate maxEndDate = allocationTimeline.stream()
                    .map(AllocationTimelineItem::getEndDate)
                    .max(LocalDate::compareTo)
                    .orElse(null);
            if (maxEndDate != null) {
                availableFrom = maxEndDate.plusDays(1);
            }
        }
        
        // Create notice info DTO
        NoticeInfoDTO noticeInfo = createNoticeInfoDTO(
            projection.getNoticeStartDate(), 
            projection.getNoticeEndDate(), 
            projection.getAllocationAllowed()
        );
        
        return ResourceTimelineResponseDTO.builder()
                .resourceId(projection.getId())
                .name(projection.getFullName())
                .avatar(generateAvatarFromName(projection.getFullName()))
                .role(projection.getDesignation())
                .skills(allSkills)
                .skillDetails(allSkillDetails)
                .certifications(certifications)
                .location(projection.getWorkingLocation())
                .experience(projection.getExperiance())
                .currentAllocation(currentAllocation)
                .availableFrom(availableFrom)
                .currentProject(currentProjects)
                .nextAssignment(null) // Placeholder - would need separate query
                .employmentType(projection.getEmploymentType())
                .allocationTimeline(allocationTimeline)
                .noticeInfo(noticeInfo)
                .build();
    }

    private AllocationTimelineItem mapToAllocationTimelineItem(AllocationTimelineProjection projection) {
        return AllocationTimelineItem.builder()
                .project(projection.getProject())
                .startDate(projection.getStartDate())
                .endDate(projection.getEndDate())
                .allocation(projection.getAllocation())
                .tentative("PLANNED".equals(projection.getAllocationStatus()))
                .build();
    }

    private String generateAvatarFromName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "UN";
        }
        
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
        } else if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return "UN";
    }

    /**
     * Creates NoticeInfoDTO from resource notice period data
     */
    private NoticeInfoDTO createNoticeInfoDTO(LocalDate noticeStartDate, LocalDate noticeEndDate, Boolean allocationAllowed) {
        LocalDate today = LocalDate.now();
        boolean isNoticePeriod = noticeStartDate != null && noticeEndDate != null 
            && !today.isBefore(noticeStartDate) && !today.isAfter(noticeEndDate);
        
        return NoticeInfoDTO.builder()
            .isNoticePeriod(isNoticePeriod)
            .noticeStartDate(noticeStartDate)
            .noticeEndDate(noticeEndDate)
            .build();
    }

    /**
     * Applies notice period throttling rules to resource availability
     * Resources in notice period have reduced effective availability
     */
    private Integer applyNoticePeriodThrottling(ResourceTimelineResponseDTO dto) {
        if (dto.getNoticeInfo() == null || !dto.getNoticeInfo().getIsNoticePeriod()) {
            return dto.getCurrentAllocation();
        }
        
        // Notice period throttling rules:
        // - If allocation is explicitly not allowed, show as 100% allocated
        // - Otherwise apply 50% throttling to current allocation
        // - Minimum throttled allocation is 50% to avoid showing false availability
        
        Integer currentAllocation = dto.getCurrentAllocation();
        
        // If resource is in notice period and allocations are not allowed
        if (dto.getNoticeInfo().getNoticeEndDate() != null) {
            LocalDate today = LocalDate.now();
            LocalDate noticeEndDate = dto.getNoticeInfo().getNoticeEndDate();
            
            // If we're within 30 days of notice end, restrict allocations heavily
            if (today.plusDays(30).isAfter(noticeEndDate) || today.isAfter(noticeEndDate)) {
                return Math.max(currentAllocation, 90); // Show as heavily allocated
            }
        }
        
        // Apply standard notice period throttling (50% reduction in effective availability)
        Integer throttledAllocation = Math.max(currentAllocation + 50, 100);
        return Math.min(throttledAllocation, 100);
    }

    /**
     * Validates if a resource can be allocated to a new project based on notice period rules
     */
    public boolean canAllocateToProject(ResourceTimelineResponseDTO dto, Integer allocationPercentage, LocalDate startDate, LocalDate endDate) {
        if (dto.getNoticeInfo() == null || !dto.getNoticeInfo().getIsNoticePeriod()) {
            return true; // No restrictions for non-notice period resources
        }
        
        LocalDate today = LocalDate.now();
        LocalDate noticeEndDate = dto.getNoticeInfo().getNoticeEndDate();
        
        // Rule 1: No new allocations after notice end date
        if (noticeEndDate != null && startDate.isAfter(noticeEndDate)) {
            return false;
        }
        
        // Rule 2: No long-term allocations (> 30 days) for notice period resources
        if (startDate != null && endDate != null) {
            long allocationDuration = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
            if (allocationDuration > 30) {
                return false;
            }
        }
        
        // Rule 3: No critical allocations (> 80% allocation) for notice period resources
        if (allocationPercentage != null && allocationPercentage > 80) {
            return false;
        }
        
        // Rule 4: Heavy restrictions within 30 days of notice end
        if (noticeEndDate != null && today.plusDays(30).isAfter(noticeEndDate)) {
            return allocationPercentage != null && allocationPercentage <= 20; // Only allow very small allocations
        }
        
        return true;
    }
}
