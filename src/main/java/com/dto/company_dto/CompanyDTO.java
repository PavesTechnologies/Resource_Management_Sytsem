package com.dto.company_dto;

import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.centralised_enums.RecordStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyDTO {
    private UUID companyId;
    private String companyName;
    private String companyCode;
    private PriorityLevel priorityLevel;
    private String countryName;
    private String defaultTimezone;
    private RecordStatus status;
    private Boolean escalationContact;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
