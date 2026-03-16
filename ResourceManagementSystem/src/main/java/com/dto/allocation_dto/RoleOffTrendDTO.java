package com.dto.allocation_dto;

import com.entity_enums.allocation_enums.RoleOffReason;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.YearMonth;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleOffTrendDTO {
    private YearMonth period;
    private RoleOffReason reason;
    private Long count;
    private String periodLabel;
}
