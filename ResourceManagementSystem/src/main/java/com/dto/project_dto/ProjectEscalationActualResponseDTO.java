package com.dto.project_dto;


import com.entity_enums.client_enums.ContactRole;
import com.entity_enums.project_enums.EscalationSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectEscalationActualResponseDTO {
    private UUID projectEscalationId;
    private String escalationLevel;
    private String contactName;
    private ContactRole contactRole;
    private String email;
    private String phone;
    private Boolean activeFlag;
    private EscalationSource source;

    private UUID contactId;

}
