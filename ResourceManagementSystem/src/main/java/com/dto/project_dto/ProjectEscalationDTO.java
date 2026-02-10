package com.dto.project_dto;

import com.entity_enums.client_enums.ContactRole;
import com.entity_enums.project_enums.EscalationLevel;
import com.entity_enums.project_enums.EscalationTriggerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectEscalationDTO {
    private UUID id;
    private Long projectId;
    private UUID clientId; // Added for creating new contact
    private UUID contactId;
    private String contactName;
    private String contactEmail;
    private String contactPhone; // Added
    private Boolean activeFlag; // Added
    private ContactRole contactRole;
    private EscalationLevel escalationLevel;
    private Set<EscalationTriggerType> triggers;
}
