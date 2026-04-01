package com.service_imple.report_service_imple;

import com.dto.report_dto.BenchPoolFilterDTO;
import com.dto.report_dto.BenchPoolReportDTO;
import com.repo.report_repo.BenchPoolReportRepository;
import com.util.report_util.RiskEvaluationHelper;
import com.entity_enums.resource_enums.EmploymentStatus;
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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BenchPoolReportService {

    private final BenchPoolReportRepository benchPoolReportRepository;
    private final RiskEvaluationHelper riskEvaluationHelper;

    public Page<BenchPoolReportDTO> getBenchPoolReport(BenchPoolFilterDTO filters) {
        log.info("Fetching bench pool report with filters: {}", filters);

        // Ensure valid pagination parameters
        int page = filters.getPage() != null && filters.getPage() >= 0 ? filters.getPage() : 0;
        int size = filters.getSize() != null && filters.getSize() > 0 ? filters.getSize() : 50;

        // Use simple pageable without custom sort since JPQL query handles ordering
        Pageable pageable = PageRequest.of(page, size);

        Page<Object[]> rawData = benchPoolReportRepository.findBenchPoolReportData(pageable);

        // Group by resourceId to handle duplicates and merge skills
        Map<Long, List<Object[]>> groupedData = rawData.getContent().stream()
                .collect(Collectors.groupingBy(row -> row[0] != null ? ((Number) row[0]).longValue() : 0L));

        List<BenchPoolReportDTO> uniqueResults = groupedData.values().stream()
                .map(this::convertGroupedRowsToDTO)
                .collect(Collectors.toList());

        // Create a new Page with the unique results
        return new PageImpl<>(uniqueResults, pageable, groupedData.size());
    }

    private BenchPoolReportDTO convertToBenchPoolReportDTO(Object[] row) {
        BenchPoolReportDTO dto = BenchPoolReportDTO.builder()
                .resourceId(row[0] != null ? ((Number) row[0]).longValue() : null)
                .name((String) row[1])
                .status(row[2] != null ? row[2].toString() : null)
                .region((String) row[3])
                .role((String) row[4])
                .cost(row[5] != null ? (BigDecimal) row[5] : BigDecimal.ZERO)
                .lastProject((String) row[7])
                .client((String) row[8])
                .build();

        // Get benchDays from query result (row[10]) instead of calculating
        Long benchDays = row[10] != null ? ((Number) row[10]).longValue() : 0L;
        dto.setBenchDays(benchDays);

        String skill = (String) row[9];
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
            return null;
        }

        // Use the first row for basic data
        Object[] firstRow = rows.get(0);
        BenchPoolReportDTO dto = BenchPoolReportDTO.builder()
                .resourceId(firstRow[0] != null ? ((Number) firstRow[0]).longValue() : null)
                .name((String) firstRow[1])
                .status(firstRow[2] != null ? firstRow[2].toString() : null)
                .region((String) firstRow[3])
                .role((String) firstRow[4])
                .cost(firstRow[5] != null ? (BigDecimal) firstRow[5] : BigDecimal.ZERO)
                .lastProject((String) firstRow[7])
                .client((String) firstRow[8])
                .build();

        // Get benchDays from query result (row[10])
        Long benchDays = firstRow[10] != null ? ((Number) firstRow[10]).longValue() : 0L;
        dto.setBenchDays(benchDays);

        // Collect all skills from all rows
        Set<String> skills = new HashSet<>();
        for (Object[] row : rows) {
            String skill = (String) row[9];
            if (skill != null && !skill.trim().isEmpty()) {
                skills.add(skill);
            }
        }
        dto.setSkills(new ArrayList<>(skills));

        riskEvaluationHelper.evaluateRisk(dto);

        return dto;
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
