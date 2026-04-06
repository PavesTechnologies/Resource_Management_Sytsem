package com.service_imple.report_service_imple;

import com.dto.report_dto.BenchPoolFilterDTO;
import com.dto.report_dto.BenchPoolReportDTO;
import com.repo.report_repo.BenchPoolReportRepository;
import com.util.report_util.RiskEvaluationHelper;
import com.entity_enums.resource_enums.EmploymentStatus;
import com.entity_enums.centralised_enums.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional(readOnly = true)
public class BenchPoolReportService {

    private static final Logger log = LoggerFactory.getLogger(BenchPoolReportService.class);
    
    private final BenchPoolReportRepository benchPoolReportRepository;
    private final RiskEvaluationHelper riskEvaluationHelper;

    public BenchPoolReportService(BenchPoolReportRepository benchPoolReportRepository, RiskEvaluationHelper riskEvaluationHelper) {
        this.benchPoolReportRepository = benchPoolReportRepository;
        this.riskEvaluationHelper = riskEvaluationHelper;
    }

    public Page<BenchPoolReportDTO> getBenchPoolReport(BenchPoolFilterDTO filters) {
        log.info("Fetching bench pool report with filters: {}", filters);

        // Ensure valid pagination parameters
        int page = filters.getPage() != null && filters.getPage() >= 0 ? filters.getPage() : 0;
        int size = filters.getSize() != null && filters.getSize() > 0 ? filters.getSize() : 50;

        // Use simple pageable without custom sort since JPQL query handles ordering
        Pageable pageable = PageRequest.of(page, size);

        Page<Object[]> rawData = benchPoolReportRepository.findBenchPoolReportData(
                filters.getSkill(),
                filters.getRole(), 
                filters.getRegion(),
                filters.getMaxCost(),
                filters.getMinBenchDays(),
                filters.getMaxBenchDays(),
                null, // Pass null for riskLevel since we'll handle it in service layer
                pageable
        );

        log.info("Raw data count: {}", rawData.getTotalElements());
        log.info("Raw data content size: {}", rawData.getContent().size());

        // Group by resourceId to handle duplicates and merge skills
        Map<Long, List<Object[]>> groupedData = rawData.getContent().stream()
                .collect(Collectors.groupingBy(row -> row[0] != null ? ((Number) row[0]).longValue() : 0L));

        log.info("Grouped data size: {}", groupedData.size());

        List<BenchPoolReportDTO> uniqueResults = groupedData.values().stream()
                .map(this::convertGroupedRowsToDTO)
                .collect(Collectors.toList());

        log.info("Unique results count before risk filtering: {}", uniqueResults.size());

        // Apply risk level filtering if specified
        if (filters.getRiskLevel() != null) {
            uniqueResults = uniqueResults.stream()
                    .filter(dto -> matchesRiskLevel(dto, filters.getRiskLevel()))
                    .collect(Collectors.toList());
            log.info("Unique results count after risk filtering: {}", uniqueResults.size());
        }

        // Create a new Page with the unique results
        return new PageImpl<>(uniqueResults, pageable, groupedData.size());
    }

    private BenchPoolReportDTO convertToBenchPoolReportDTO(Object[] row) {
        BenchPoolReportDTO dto = BenchPoolReportDTO.builder()
                .resourceId(row[0] != null ? ((Number) row[0]).longValue() : null)
                .name(safeToString(row[1]))
                .status(safeToString(row[2]))
                .region(safeToString(row[3]))
                .role(safeToString(row[4]))
                .cost(row[5] != null ? (BigDecimal) row[5] : BigDecimal.ZERO)
                .lastProject(safeToString(row[7]))
                .client(safeToString(row[8]))
                .build();

        // Calculate benchDays correctly
        // Extract allocationEndDate from row data
        LocalDate allocationEndDate = convertToLocalDate(row[6]);
        
        log.debug("Date conversion - allocationEndDate: {}", allocationEndDate);
        
        // If allocationEndDate is in past, resource is on bench from that date
        // If allocationEndDate is in future or null, resource is currently allocated (bench days = 0)
        LocalDate benchStartDate = null;
        if (allocationEndDate != null && allocationEndDate.isBefore(LocalDate.now())) {
            benchStartDate = allocationEndDate;
        }
        
        if (benchStartDate != null) {
            long benchDays = ChronoUnit.DAYS.between(benchStartDate, LocalDate.now());
            dto.setBenchDays(benchDays);
        } else {
            dto.setBenchDays(0L);
        }

        Object skillObj = row[9]; // skill is now at index 9
        String skill = safeToString(skillObj);
        if (skill != null && !skill.trim().isEmpty()) {
            dto.setSkills(List.of(skill));
        } else {
            dto.setSkills(new ArrayList<>());
        }

        riskEvaluationHelper.evaluateRisk(dto);

        return dto;
    }

    private BenchPoolReportDTO convertGroupedRowsToDTO(List<Object[]> rows) {
        if (rows.isEmpty()) {
            log.warn("Empty rows provided to convertGroupedRowsToDTO");
            return null;
        }

        log.debug("Converting grouped rows for resource, row count: {}", rows.size());

        // Use the first row for basic data
        Object[] firstRow = rows.get(0);
        BenchPoolReportDTO dto = BenchPoolReportDTO.builder()
                .resourceId(firstRow[0] != null ? ((Number) firstRow[0]).longValue() : null)
                .name(safeToString(firstRow[1]))
                .status(safeToString(firstRow[2]))
                .region(safeToString(firstRow[3]))
                .role(safeToString(firstRow[4]))
                .cost(firstRow[5] != null ? (BigDecimal) firstRow[5] : BigDecimal.ZERO)
                .lastProject(safeToString(firstRow[7]))
                .client(safeToString(firstRow[8]))
                .build();

        log.debug("Resource data: ID={}, Name={}, Status={}", dto.getResourceId(), dto.getName(), dto.getStatus());

        // Calculate benchDays correctly
        // Extract dates from the row data
        LocalDate allocationEndDate = convertToLocalDate(firstRow[6]);
        
        log.debug("Date conversion - allocationEndDate: {}", allocationEndDate);
        
        // If allocationEndDate is in the past, resource is on bench from that date
        // If allocationEndDate is in future or null, resource is currently allocated (bench days = 0)
        LocalDate benchStartDate = null;
        if (allocationEndDate != null && allocationEndDate.isBefore(LocalDate.now())) {
            benchStartDate = allocationEndDate;
        }
        
        if (benchStartDate != null) {
            long benchDays = ChronoUnit.DAYS.between(benchStartDate, LocalDate.now());
            dto.setBenchDays(benchDays);
            log.debug("Calculated bench days: {}", benchDays);
        } else {
            dto.setBenchDays(0L);
            log.debug("Resource is currently allocated, setting bench days to 0");
        }

        // Collect all skills from all rows (skill is now at index 9)
        Set<String> skills = new HashSet<>();
        for (Object[] row : rows) {
            String skill = safeToString(row[9]);
            if (skill != null && !skill.trim().isEmpty()) {
                skills.add(skill);
            }
        }
        dto.setSkills(new ArrayList<>(skills));
        log.debug("Skills found: {}", dto.getSkills());

        riskEvaluationHelper.evaluateRisk(dto);

        return dto;
    }

    private String safeToString(Object obj) {
        if (obj == null) {
            return null;
        }
        return obj.toString();
    }

    private boolean matchesRiskLevel(BenchPoolReportDTO dto, RiskLevel riskLevel) {
        if (dto.getBenchDays() == null) {
            return false;
        }
        
        long benchDays = dto.getBenchDays();
        BigDecimal cost = dto.getCost() != null ? dto.getCost() : BigDecimal.ZERO;
        
        switch (riskLevel) {
            case HIGH:
                return benchDays > 30 || (cost.compareTo(new BigDecimal("80000")) > 0 && benchDays > 20);
            case MEDIUM:
                return benchDays > 15;
            case LOW:
                return benchDays <= 15;
            case CRITICAL:
                return benchDays > 60; // Adding critical level for very high bench days
            default:
                return true;
        }
    }

    private LocalDate convertToLocalDate(Object dateValue) {
        if (dateValue == null) {
            return null;
        }
        
        if (dateValue instanceof java.sql.Date) {
            return ((java.sql.Date) dateValue).toLocalDate();
        } else if (dateValue instanceof java.time.LocalDate) {
            return (java.time.LocalDate) dateValue;
        } else if (dateValue instanceof String) {
            try {
                return java.time.LocalDate.parse((String) dateValue);
            } catch (Exception e) {
                log.warn("Failed to parse date from string: {}", dateValue, e);
                return null;
            }
        } else {
            log.warn("Unexpected date type: {}", dateValue.getClass().getName());
            return null;
        }
    }

    public List<BenchPoolReportDTO> getBenchPoolReportForExport(BenchPoolFilterDTO filters) {
        log.info("Fetching bench pool report for export with filters: {}", filters);

        BenchPoolFilterDTO exportFilters = filters.toBuilder()
                .page(0)
                .size(Integer.MAX_VALUE)
                .build();

        Page<BenchPoolReportDTO> allData = getBenchPoolReport(exportFilters);
        return allData.getContent();
    }
}
