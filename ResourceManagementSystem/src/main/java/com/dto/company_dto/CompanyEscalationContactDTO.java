package com.dto.company_dto;

import com.entity_enums.client_enums.ContactRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyEscalationContactDTO {
    private UUID contactId;
    private UUID companyId;
    private String contactName;
    private ContactRole contactRole;
    private String email;
    private String phone;
    private Boolean activeFlag;
    private String escalationLevel;
}
