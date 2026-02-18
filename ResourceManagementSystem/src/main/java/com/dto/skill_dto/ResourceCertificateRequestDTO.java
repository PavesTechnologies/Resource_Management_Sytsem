package com.dto.skill_dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class ResourceCertificateRequestDTO {
    @JsonProperty("resourceId")
    private Long resourceId;
    
    @JsonProperty("skillId")
    private UUID skillId; // certification skill
    
    @JsonProperty("proficiencyId")
    private UUID proficiencyId;
    
    @JsonProperty("expiryDate")
    private LocalDate expiryDate;
}
