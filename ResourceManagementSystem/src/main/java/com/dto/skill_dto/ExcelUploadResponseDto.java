package com.dto.skill_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ExcelUploadResponseDto {
    private int totalRows;

    private int validRows;

    private int invalidRows;

    private int duplicateRows;

    private int existingRows;

    private List<RowSuccessDto> existingRecords;

    private List<RowSuccessDto> validRecords;

    private List<RowErrorDto> errors;

    private SkillTaxonomyResponseDto savedData;

    private SkillTaxonomyRequestDto uploadData;
}
