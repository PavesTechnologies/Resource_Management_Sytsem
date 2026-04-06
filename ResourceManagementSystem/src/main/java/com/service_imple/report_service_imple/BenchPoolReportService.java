package com.service_imple.report_service_imple;

import com.dto.report_dto.BenchPoolFilterDTO;
import com.dto.report_dto.BenchPoolReportDTO;
import com.dto.bench_dto.BenchPoolResponseDTO;
import com.service_imple.bench_service_impl.BenchService;
import com.entity_enums.bench.SubState;
import com.entity_enums.centralised_enums.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
public class BenchPoolReportService {

    private final BenchService benchService;
    
    public BenchPoolReportService(BenchService benchService) {
        this.benchService = benchService;
    }

    public Page<BenchPoolReportDTO> getBenchPoolReport(BenchPoolFilterDTO filters) {
        log.info("Fetching bench pool report with filters: {}", filters);

        // Get bench and pool resources using same logic as working endpoints
        List<BenchPoolResponseDTO> benchResources = benchService.getBenchResources();
        List<BenchPoolResponseDTO> poolResources = benchService.getPoolResources();

        // Combine both lists
        List<BenchPoolResponseDTO> allResources = new ArrayList<>();
        allResources.addAll(benchResources);
        allResources.addAll(poolResources);

        // Apply filters
        List<BenchPoolResponseDTO> filteredResources = applyFilters(allResources, filters);

        // Convert to report DTOs
        List<BenchPoolReportDTO> reportData = filteredResources.stream()
                .map(this::convertToReportDTO)
                .collect(Collectors.toList());

        // Apply pagination
        int page = filters.getPage() != null && filters.getPage() >= 0 ? filters.getPage() : 0;
        int size = filters.getSize() != null && filters.getSize() > 0 ? filters.getSize() : 50;
        
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, reportData.size());
        
        List<BenchPoolReportDTO> pageContent = reportData.subList(startIndex, endIndex);
        
        Pageable pageable = PageRequest.of(page, size);
        return new PageImpl<>(pageContent, pageable, reportData.size());
    }

    public List<BenchPoolReportDTO> getBenchPoolReportForExport(BenchPoolFilterDTO filters) {
        log.info("Fetching bench pool report for export with filters: {}", filters);

        // For export, get all data without pagination
        BenchPoolFilterDTO exportFilters = filters.toBuilder()
                .page(0)
                .size(Integer.MAX_VALUE)
                .build();

        Page<BenchPoolReportDTO> allData = getBenchPoolReport(exportFilters);
        return allData.getContent();
    }

    private List<BenchPoolResponseDTO> applyFilters(List<BenchPoolResponseDTO> resources, BenchPoolFilterDTO filters) {
        return resources.stream()
                .filter(resource -> {
                    // Skill filter
                    if (filters.getSkill() != null && !filters.getSkill().isEmpty()) {
                        boolean hasSkill = resource.getSkillGroups().stream()
                                .anyMatch(skillMap -> skillMap.containsKey(filters.getSkill()));
                        if (!hasSkill) return false;
                    }

                    // Role filter
                    if (filters.getRole() != null && !filters.getRole().isEmpty()) {
                        if (!filters.getRole().equalsIgnoreCase(resource.getDesignation())) {
                            return false;
                        }
                    }

                    // Region/Location filter
                    if (filters.getRegion() != null && !filters.getRegion().isEmpty()) {
                        if (!filters.getRegion().equalsIgnoreCase(resource.getLocation())) {
                            return false;
                        }
                    }

                    // Cost filter
                    if (filters.getMaxCost() != null) {
                        if (resource.getCostPerDay() == null || 
                            BigDecimal.valueOf(resource.getCostPerDay()).compareTo(filters.getMaxCost()) > 0) {
                            return false;
                        }
                    }

                    // Bench days filter
                    if (filters.getMinBenchDays() != null) {
                        if (resource.getAging() == null || resource.getAging() < filters.getMinBenchDays()) {
                            return false;
                        }
                    }

                    if (filters.getMaxBenchDays() != null) {
                        if (resource.getAging() == null || resource.getAging() > filters.getMaxBenchDays()) {
                            return false;
                        }
                    }

                    // Risk level filter
                    if (filters.getRiskLevel() != null) {
                        RiskLevel resourceRiskLevel = calculateRiskLevel(resource.getAging(), resource.getCostPerDay());
                        if (!resourceRiskLevel.equals(filters.getRiskLevel())) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    private BenchPoolReportDTO convertToReportDTO(BenchPoolResponseDTO source) {
        // Determine resource type based on subState
        String resourceType = "BENCH";
        if (source.getSubState() == SubState.RND) {
            resourceType = "INTERNAL_POOL";
        }

        // Calculate risk level
        RiskLevel riskLevel = calculateRiskLevel(source.getAging(), source.getCostPerDay());

        return BenchPoolReportDTO.builder()
                .resourceId(source.getEmployeeId())
                .name(source.getResourceName())
                .status(resourceType)
                .region(source.getLocation())
                .role(source.getDesignation())
                .cost(BigDecimal.valueOf(source.getCostPerDay() != null ? source.getCostPerDay() : 0.0))
                .benchDays(source.getAging() != null ? source.getAging().longValue() : 0L)
                .lastProject(formatSkills(source.getSkillGroups()))
                .client(source.getLastAllocationDate() != null ? source.getLastAllocationDate().toString() : "")
                .riskLevel(riskLevel)
                .skills(formatSkillsToList(source.getSkillGroups()))
                .build();
    }

    private RiskLevel calculateRiskLevel(Integer aging, Double costPerDay) {
        if (aging == null) aging = 0;
        if (costPerDay == null) costPerDay = 0.0;

        if (aging > 60) {
            return RiskLevel.CRITICAL;
        } else if (aging > 30 || (costPerDay > 300 && aging > 20)) {
            return RiskLevel.HIGH;
        } else if (aging > 15) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.LOW;
        }
    }

    private String formatSkills(List<Map<String, String>> skillGroups) {
        if (skillGroups == null || skillGroups.isEmpty()) {
            return "";
        }
        
        StringBuilder skills = new StringBuilder();
        for (Map<String, String> skillMap : skillGroups) {
            for (Map.Entry<String, String> entry : skillMap.entrySet()) {
                if (skills.length() > 0) {
                    skills.append(", ");
                }
                skills.append(entry.getKey()).append(" (").append(entry.getValue()).append(")");
            }
        }
        return skills.toString();
    }

    private List<String> formatSkillsToList(List<Map<String, String>> skillGroups) {
        if (skillGroups == null || skillGroups.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> skills = new ArrayList<>();
        for (Map<String, String> skillMap : skillGroups) {
            for (Map.Entry<String, String> entry : skillMap.entrySet()) {
                skills.add(entry.getKey() + " (" + entry.getValue() + ")");
            }
        }
        return skills;
    }
}
