package com.dto.resource_dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

@Data
public class ResourceRemovalDTO {
    
    @NotNull(message = "Resource ID is required")
    private Long resourceId;
    
    @NotNull(message = "Notice period end date is required")
    @FutureOrPresent(message = "Notice period end date must be today or in the future")
    private LocalDate noticePeriodEndDate;
    
    @NotBlank(message = "Removal reason is required")
    private String removalReason;
    
    private String additionalComments;
    
    private Boolean triggerAttritionImmediately = false;
    
    private String processedBy;
}
