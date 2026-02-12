package com.dto.project_dto;

import com.entity_enums.client_enums.ContactRole;
import com.entity_enums.project_enums.EscalationLevel;
import com.entity_enums.project_enums.EscalationSource;
import com.entity_enums.project_enums.EscalationTriggerType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

//this is request dto for adding escalation contact///////////////////////
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectEscalationResponseDTO {
    @NotNull(message = "Project ID is required")
    private Long projectId;
    @NotNull(message = "Type is required")
    private String type;
    private String escalationLevel;
    private String contactName;
    private ContactRole contactRole;
    private String email;
    private String phone;
    private Boolean activeFlag;
//    private Set<EscalationTriggerType> triggers;
    private EscalationSource source;
    private Set<UUID> contactId;
}
