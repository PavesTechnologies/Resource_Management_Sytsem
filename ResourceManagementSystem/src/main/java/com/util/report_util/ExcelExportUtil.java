package com.util.report_util;

import com.dto.report_dto.BenchPoolReportDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
public class ExcelExportUtil {


    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public byte[] exportBenchPoolReportToExcel(List<BenchPoolReportDTO> data, String reportTitle) throws IOException {
        log.info("Creating Excel export for bench pool report with {} records", data.size());

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(reportTitle);

            createHeaderRow(workbook, sheet);
            populateDataRows(workbook, sheet, data);
            autoSizeColumns(sheet);

            workbook.write(outputStream);
            byte[] result = outputStream.toByteArray();

            log.info("Excel export created successfully with {} bytes", result.length);
            return result;

        } catch (IOException e) {
            log.error("Error creating Excel export", e);
            throw e;
        }
    }

    private void createHeaderRow(XSSFWorkbook workbook, Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        String[] headers = {
                "Name", "Status", "Skills", "Role", "Region",
                "Client", "Last Project", "Bench Days", "Cost",
                "Risk Level", "Risk Type", "Recommended Action"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void populateDataRows(XSSFWorkbook workbook, Sheet sheet, List<BenchPoolReportDTO> data) {
        CellStyle dataStyle = createDataStyle(workbook);

        int rowNum = 1;
        for (BenchPoolReportDTO record : data) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(record.getName() != null ? record.getName() : "");
            row.createCell(1).setCellValue(record.getStatus() != null ? record.getStatus() : "");
            row.createCell(2).setCellValue(String.join(", ", record.getSkills() != null ? record.getSkills() : List.of()));
            row.createCell(3).setCellValue(record.getRole() != null ? record.getRole() : "");
            row.createCell(4).setCellValue(record.getRegion() != null ? record.getRegion() : "");
            row.createCell(5).setCellValue(record.getClient() != null ? record.getClient() : "");
            row.createCell(6).setCellValue(record.getLastProject() != null ? record.getLastProject() : "");
            row.createCell(7).setCellValue(record.getBenchDays() != null ? record.getBenchDays() : 0);
            row.createCell(8).setCellValue(record.getCost() != null ? record.getCost().doubleValue() : 0.0);
            row.createCell(9).setCellValue(record.getRiskLevel() != null ? record.getRiskLevel().toString() : "");
            row.createCell(10).setCellValue(record.getRiskType() != null ? record.getRiskType() : "");
            row.createCell(11).setCellValue(record.getRecommendedAction() != null ? record.getRecommendedAction() : "");

            for (int i = 0; i < 12; i++) {
                row.getCell(i).setCellStyle(dataStyle);
            }
        }
    }

    private CellStyle createDataStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    private void autoSizeColumns(Sheet sheet) {
        for (int i = 0; i < 12; i++) {
            sheet.autoSizeColumn(i);
            int maxWidth = Math.min(sheet.getColumnWidth(i) * 2, 15000);
            sheet.setColumnWidth(i, maxWidth);
        }
    }
}
