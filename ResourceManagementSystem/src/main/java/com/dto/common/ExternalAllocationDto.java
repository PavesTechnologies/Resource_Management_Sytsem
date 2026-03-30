package com.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalAllocationDto {
    private Long allocationId;
    private Long resourceId;
    private Long projectId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer percentage;
    private String status;
    private String allocationType;

    public boolean isConfirmed() {
        return "ACTIVE".equalsIgnoreCase(status) || "APPROVED".equalsIgnoreCase(status);
    }

    public boolean isDraft() {
        return "PLANNED".equalsIgnoreCase(status) || "DRAFT".equalsIgnoreCase(status);
    }
}
