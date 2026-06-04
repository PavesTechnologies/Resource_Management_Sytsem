package com.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for parsing Excel files containing serial numbers.
 * Supports both .xlsx and .xls formats.
 * Uses Apache POI for efficient Excel processing.
 */
@Slf4j
public class ExcelSerialNumberParser {

    /**
     * Parse serial numbers from Excel file.
     * 
     * @param file MultipartFile containing Excel data
     * @return List of serial numbers from Excel file
     * @throws Exception If file format is invalid or parsing fails
     */
    public static List<String> parseSerialNumbers(MultipartFile file) throws Exception {
        validateExcelFile(file);
        
        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = createWorkbook(inputStream, file.getOriginalFilename());
            Sheet sheet = workbook.getSheetAt(0); // First sheet only
            
            return extractSerialNumbers(sheet);
            
        } catch (IOException e) {
            log.error("Failed to parse Excel file: {}", file.getOriginalFilename(), e);
            throw new Exception("Failed to read Excel file: " + e.getMessage());
        }
    }

    /**
     * Validate that uploaded file is a valid Excel file.
     * 
     * @param file MultipartFile to validate
     * @throws Exception If file is not a valid Excel format
     */
    private static void validateExcelFile(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new Exception("Excel file is required");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new Exception("Invalid file format. Only .xlsx and .xls files are supported");
        }

        // Check file size (max 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new Exception("Excel file size must be less than 10MB");
        }
    }

    /**
     * Create appropriate Workbook based on file extension.
     * 
     * @param inputStream Input stream from file
     * @param filename Original filename
     * @return Workbook instance
     * @throws IOException If workbook creation fails
     */
    private static Workbook createWorkbook(InputStream inputStream, String filename) throws IOException {
        if (filename.endsWith(".xlsx")) {
            return new XSSFWorkbook(inputStream);
        } else if (filename.endsWith(".xls")) {
            return new HSSFWorkbook(inputStream);
        } else {
            throw new IOException("Unsupported file format: " + filename);
        }
    }

    /**
     * Extract serial numbers from Excel sheet.
     * Assumes first column contains serial numbers and first row is header.
     * 
     * @param sheet Excel sheet to parse
     * @return List of serial numbers
     * @throws Exception If parsing fails
     */
    private static List<String> extractSerialNumbers(Sheet sheet) throws Exception {
        List<String> serialNumbers = new ArrayList<>();
        
        if (sheet.getPhysicalNumberOfRows() <= 1) {
            throw new Exception("Excel file must contain at least one data row (excluding header)");
        }

        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new Exception("Excel file must have a header row");
        }

        // Find first column that contains "Serial" in header
        int serialColumnIndex = findSerialColumnIndex(headerRow);
        if (serialColumnIndex == -1) {
            throw new Exception("Excel file must have a 'Serial Number' column in the first row");
        }

        // Process data rows (skip header row)
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                Cell cell = row.getCell(serialColumnIndex);
                String serialNumber = getCellValueAsString(cell);
                
                if (serialNumber != null && !serialNumber.trim().isEmpty()) {
                    serialNumbers.add(serialNumber.trim());
                }
            }
        }

        log.info("Successfully parsed {} serial numbers from Excel file", serialNumbers.size());
        return serialNumbers;
    }

    /**
     * Find column index that contains serial numbers.
     * Looks for "Serial" in header cell values.
     * 
     * @param headerRow First row of Excel sheet
     * @return Column index for serial numbers, or -1 if not found
     */
    private static int findSerialColumnIndex(Row headerRow) {
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            String headerValue = getCellValueAsString(cell);
            
            if (headerValue != null && 
                (headerValue.toLowerCase().contains("serial") || 
                 headerValue.toLowerCase().contains("s/n") ||
                 headerValue.toLowerCase().contains("no"))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get cell value as string, handling different cell types.
     * 
     * @param cell Excel cell
     * @return String value of cell
     */
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
