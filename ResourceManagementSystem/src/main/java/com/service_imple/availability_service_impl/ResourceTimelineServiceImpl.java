package com.service_imple.availability_service_impl;

import com.dto.ResourceTimelineDTO;
import com.dto.ResourceTimelineResponseDTO;
import com.dto.ResourceTimelineApiResponse;
import com.dto.allocation_dto.AllocationTimelineItem;
import com.service_imple.availability_service_impl.projection.ResourceTimelineProjection;
import com.service_imple.availability_service_impl.projection.TimelineKpiProjection;
import com.service_imple.availability_service_impl.projection.AllocationTimelineProjection;
import com.service_imple.availability_service_impl.projection.CurrentProjectProjection;
import com.service_imple.availability_service_impl.projection.CurrentAllocationProjection;
import com.repo.timeline_repo.ResourceTimelineRepository;
import com.service_interface.availability_service_interface.ResourceTimelineService;
import com.service_imple.external_api_impl.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceTimelineServiceImpl implements ResourceTimelineService {

    private final ResourceTimelineRepository resourceTimelineRepository;
    private final TokenService tokenService;

    @Override
    public List<ResourceTimelineDTO> getAllResourceTimelines() {
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceTimelineApiResponse getResourceTimelineWindow(
            LocalDate startDate,
            LocalDate endDate,
            String designation,
            String location,
            Integer minExp,
            Integer maxExp,
            String employmentType,
            String status,
            Integer page,
            Integer size) {
        
        // Validate date requirements
        if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
            return ResourceTimelineApiResponse.error("Both startDate and endDate must be provided together, or both must be null");
        }
        
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            return ResourceTimelineApiResponse.error("startDate must not be after endDate");
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
                    startDate, endDate, designation, location, employmentType, minExp, maxExp, size, page * size);
            
            totalCount = resourceTimelineRepository.getResourceTimelineWindowCount(
                    startDate, endDate, designation, location, employmentType, minExp, maxExp);
        } else {
            // Full History Mode - use date-agnostic queries
            filteredData = resourceTimelineRepository.getResourceTimelineFullHistory(
                    designation, location, employmentType, minExp, maxExp, size, page * size);
            
            totalCount = resourceTimelineRepository.getResourceTimelineFullHistoryCount(
                    designation, location, employmentType, minExp, maxExp);
        }
        
        // Fetch allocation timeline, current projects, and current allocations for all resources
        final Map<Long, List<AllocationTimelineItem>> allocationTimelineMap;
        final Map<Long, List<String>> currentProjectMap;
        final Map<Long, Integer> currentAllocationMap;
        
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
        } else {
            allocationTimelineMap = Map.of();
            currentProjectMap = Map.of();
            currentAllocationMap = Map.of();
        }
        
        // Map to full response DTOs with status filtering
        List<ResourceTimelineResponseDTO> responseDTOs = filteredData.stream()
                .map(projection -> mapToFullResponseDTO(projection, allocationTimelineMap, currentProjectMap, currentAllocationMap))
                .filter(dto -> {
                    if (status == null) return true;
                    return switch (status) {
                        case "available" -> dto.getCurrentAllocation() <= 20;
                        case "partial" -> dto.getCurrentAllocation() > 20 && dto.getCurrentAllocation() <= 70;
                        case "allocated" -> dto.getCurrentAllocation() > 70;
                        default -> true;
                    };
                })
                .collect(Collectors.toList());
        
        return ResourceTimelineApiResponse.success("Timeline fetched successfully", responseDTOs, page, size, totalCount);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceTimelineResponseDTO.TimelineKPI getTimelineKPI(
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
                    startDate, endDate, designation, location, employmentType, minExp, maxExp);
        } else {
            // Overall KPI
            kpiData = resourceTimelineRepository.getTimelineKPIFullHistory(
                    designation, location, employmentType, minExp, maxExp);
        }
        
        if (kpiData.isEmpty()) {
            return ResourceTimelineResponseDTO.TimelineKPI.builder()
                    .totalResources(0L)
                    .fullyAvailable(0L)
                    .partiallyAvailable(0L)
                    .fullyAllocated(0L)
                    .overAllocated(0L)
                    .utilization(0.0)
                    .build();
        }
        
        TimelineKpiProjection kpi = kpiData.get(0);
        return ResourceTimelineResponseDTO.TimelineKPI.builder()
                .totalResources(kpi.getTotalResources())
                .fullyAvailable(kpi.getFullyAvailable())
                .partiallyAvailable(kpi.getPartiallyAvailable())
                .fullyAllocated(kpi.getFullyAllocated())
                .overAllocated(kpi.getOverAllocated())
                .utilization(kpi.getUtilization() != null ? kpi.getUtilization() : 0.0)
                .build();
    }

    private ResourceTimelineResponseDTO mapToFullResponseDTO(
            ResourceTimelineProjection projection,
            Map<Long, List<AllocationTimelineItem>> allocationTimelineMap,
            Map<Long, List<String>> currentProjectMap,
            Map<Long, Integer> currentAllocationMap) {
        
        List<AllocationTimelineItem> allocationTimeline = allocationTimelineMap.getOrDefault(
                projection.getId(), List.of());
        
        List<String> currentProjects = currentProjectMap.getOrDefault(
                projection.getId(), List.of());
        
        // Use current allocation from today-based calculation, fallback to 0 if not found
        Integer currentAllocation = currentAllocationMap.getOrDefault(projection.getId(), 0);
        
        // Call utilization API and set utilization to null if any data is returned
        List<Integer> utilizationHistory = List.of(0);
        try {
            YearMonth currentMonth = YearMonth.now();
            Map<String, Object> utilizationData = tokenService.getMonthlyUtilization(
                projection.getId(), currentMonth.getYear(), currentMonth.getMonthValue());
            
            if (utilizationData != null && !utilizationData.isEmpty()) {
                // If utilization API returns any data, set utilization to null
                utilizationHistory = null;
            } else {
                // Use current allocation if no utilization data returned
                utilizationHistory = currentAllocation > 0 ? List.of(currentAllocation) : List.of(0);
            }
        } catch (Exception e) {
            // On any error, use current allocation
            utilizationHistory = currentAllocation > 0 ? List.of(currentAllocation) : List.of(0);
        }
        
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
        
        return ResourceTimelineResponseDTO.builder()
                .resourceId(projection.getId())
                .name(projection.getFullName())
                .avatar(generateAvatarFromName(projection.getFullName()))
                .role(projection.getDesignation())
                .skills(List.of()) // Placeholder - would need separate query
                .location(projection.getWorkingLocation())
                .experience(projection.getExperiance())
                .currentAllocation(currentAllocation)
                .availableFrom(availableFrom)
                .currentProject(currentProjects)
                .nextAssignment(null) // Placeholder - would need separate query
                .employmentType(projection.getEmploymentType())
                .utilizationHistory(utilizationHistory)
                .allocationTimeline(allocationTimeline)
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
}
