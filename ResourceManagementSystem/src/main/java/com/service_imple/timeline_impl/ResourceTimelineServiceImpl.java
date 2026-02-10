package com.service_imple.timeline_impl;

import com.dto.ResourceTimelineDTO;
import com.repo.timeline_repo.ResourceTimelineRepository;
import com.service_interface.timeline_interface.ResourceTimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ResourceTimelineServiceImpl implements ResourceTimelineService {

    private final ResourceTimelineRepository resourceTimelineRepository;

    @Override
    public List<ResourceTimelineDTO> getAllResourceTimelines() {
        List<Object[]> summaryData = resourceTimelineRepository.getResourceTimelineSummary();
        
        return summaryData.stream()
                .map(this::mapToResourceTimelineDTO)
                .collect(Collectors.toList());
    }

    private ResourceTimelineDTO mapToResourceTimelineDTO(Object[] row) {
        Long resourceId = ((Number) row[0]).longValue();
        String name = (String) row[1];
        String avatar = (String) row[2];
        String role = (String) row[3];
        String location = (String) row[4];
        Integer experience = row[5] != null ? ((Number) row[5]).intValue() : 0;
        String employmentType = (String) row[6];
        String nextAssignment = (String) row[10];

        List<String> skills = getResourceSkills(resourceId);
        List<Integer> utilizationHistory = getUtilizationHistory(resourceId);
        List<ResourceTimelineDTO.AllocationTimelineItem> allocationTimeline = getAllocationTimeline(resourceId);
        List<String> currentProjects = getCurrentProjects(resourceId);
        LocalDate availableFrom = calculateAvailableFrom(resourceId);
        Integer currentAllocation = calculateCurrentAllocation(resourceId);

        return ResourceTimelineDTO.builder()
                .id(resourceId.toString())
                .name(name)
                .avatar(avatar != null && !avatar.isEmpty() ? avatar : generateAvatarFromName(name))
                .role(role)
                .skills(skills)
                .location(location)
                .experience(experience)
                .currentAllocation(currentAllocation)
                .availableFrom(availableFrom)
                .currentProject(currentProjects)
                .nextAssignment(nextAssignment)
                .employmentType(employmentType)
                .utilizationHistory(utilizationHistory)
                .allocationTimeline(allocationTimeline)
                .build();
    }

    private List<String> getResourceSkills(Long resourceId) {
        Optional<String> skillsOpt = resourceTimelineRepository.getResourceSkills(resourceId);
        if (skillsOpt.isPresent() && !skillsOpt.get().trim().isEmpty()) {
            return Arrays.stream(skillsOpt.get().split(","))
                    .map(String::trim)
                    .filter(skill -> !skill.isEmpty())
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private List<Integer> getUtilizationHistory(Long resourceId) {
        List<Integer> history = resourceTimelineRepository.getUtilizationHistory(resourceId);
        return history != null ? history : new ArrayList<>();
    }

    private Integer calculateCurrentAllocation(Long resourceId) {
        List<Object[]> allocationData = resourceTimelineRepository.getAllocationTimeline(resourceId);
        if (allocationData.isEmpty()) {
            return 0;
        }
        
        // Sum allocations for current date and future dates
        LocalDate today = LocalDate.now();
        return allocationData.stream()
                .filter(row -> {
                    LocalDate startDate = ((java.sql.Date) row[1]).toLocalDate();
                    LocalDate endDate = ((java.sql.Date) row[2]).toLocalDate();
                    return !endDate.isBefore(today); // Include current and future allocations
                })
                .mapToInt(row -> ((Number) row[3]).intValue())
                .sum();
    }

    private List<String> getCurrentProjects(Long resourceId) {
        List<ResourceTimelineDTO.AllocationTimelineItem> allocationTimeline = getAllocationTimeline(resourceId);
        return allocationTimeline.stream()
                .filter(item -> !item.getTentative()) // Only non-tentative (ACTIVE) projects
                .map(ResourceTimelineDTO.AllocationTimelineItem::getProject)
                .collect(Collectors.toList());
    }

    private LocalDate calculateAvailableFrom(Long resourceId) {
        List<Object[]> allocationData = resourceTimelineRepository.getAllocationTimeline(resourceId);
        if (allocationData.isEmpty()) {
            return null;
        }
        
        // Find the latest end date
        LocalDate latestEndDate = allocationData.stream()
                .map(row -> ((java.sql.Date) row[2]).toLocalDate())
                .max(LocalDate::compareTo)
                .orElse(null);
        
        // Available from is the day after the last project ends
        return latestEndDate != null ? latestEndDate.plusDays(1) : null;
    }

    private List<ResourceTimelineDTO.AllocationTimelineItem> getAllocationTimeline(Long resourceId) {
        List<Object[]> timelineData = resourceTimelineRepository.getAllocationTimeline(resourceId);
        
        return timelineData.stream()
                .map(row -> ResourceTimelineDTO.AllocationTimelineItem.builder()
                        .project((String) row[0])
                        .startDate(((java.sql.Date) row[1]).toLocalDate())
                        .endDate(((java.sql.Date) row[2]).toLocalDate())
                        .allocation(((Number) row[3]).intValue())
                        .tentative(((Number) row[4]).intValue() == 1)
                        .build())
                .collect(Collectors.toList());
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
