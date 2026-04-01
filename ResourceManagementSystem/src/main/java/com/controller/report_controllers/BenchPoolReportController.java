package com.controller.report_controllers;

import com.dto.centralised_dto.ApiResponse;
import com.dto.report_dto.BenchPoolFilterDTO;
import com.dto.report_dto.BenchPoolReportDTO;
import com.service_imple.report_service_imple.BenchPoolReportService;
import com.util.report_util.ExcelExportUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", allowCredentials = "false")
@Tag(name = "Bench Pool Reports", description = "APIs for bench and internal pool reporting with risk analysis")
public class BenchPoolReportController {

    private final BenchPoolReportService benchPoolReportService;
    private final ExcelExportUtil excelExportUtil;

    @GetMapping("/bench-pool")
    @Operation(summary = "Get bench pool report with risk evaluation", 
               description = "Fetches all resources with BENCH or INTERNAL_POOL status, calculates risk levels, and applies filters")
    // @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RESOURCE_MANAGER', 'HR')")
    public ResponseEntity<ApiResponse<Page<BenchPoolReportDTO>>> getBenchPoolReport(
            @Parameter(description = "Filter parameters for bench pool report")
            @Valid @ModelAttribute BenchPoolFilterDTO filters) {
        
        log.info("Fetching bench pool report with filters: {}", filters);
        
        try {
            Page<BenchPoolReportDTO> reportData = benchPoolReportService.getBenchPoolReport(filters);
            
            ApiResponse<Page<BenchPoolReportDTO>> response = ApiResponse.success(
                    "Bench pool report retrieved successfully", 
                    reportData
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching bench pool report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error fetching bench pool report: " + e.getMessage()));
        }
    }

    @GetMapping("/bench-pool/export")
    @Operation(summary = "Export bench pool report to Excel", 
               description = "Exports the same data as the bench pool report API to an Excel file with formatting")
    // @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RESOURCE_MANAGER', 'HR')")
    public ResponseEntity<ByteArrayResource> exportBenchPoolReport(
            @Parameter(description = "Filter parameters for export (same as report API)")
            @Valid @ModelAttribute BenchPoolFilterDTO filters) {
        
        log.info("Exporting bench pool report with filters: {}", filters);
        
        try {
            List<BenchPoolReportDTO> reportData = benchPoolReportService.getBenchPoolReportForExport(filters);
            
            String fileName = "Bench_Pool_Report_" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            
            byte[] excelData = excelExportUtil.exportBenchPoolReportToExcel(reportData, "Bench Pool Report");
            
            ByteArrayResource resource = new ByteArrayResource(excelData);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
            headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(excelData.length)
                    .body(resource);
            
        } catch (IOException e) {
            log.error("Error exporting bench pool report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/bench-pool/filters")
    @Operation(summary = "Get available filter options", 
               description = "Returns available options for filters like skills, roles, regions")
    // @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RESOURCE_MANAGER', 'HR')")
    public ResponseEntity<ApiResponse<Object>> getFilterOptions() {
        
        log.info("Fetching filter options for bench pool report");
        
        try {
            ApiResponse<Object> response = ApiResponse.success("Filter options retrieved successfully", null);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching filter options", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error fetching filter options: " + e.getMessage()));
        }
    }
}
