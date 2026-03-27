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
public class ExternalAllocationResponse {
    private Set<ExternalAllocationDto> allocations;
    private Long resourceId;
    private LocalDate date;
}
