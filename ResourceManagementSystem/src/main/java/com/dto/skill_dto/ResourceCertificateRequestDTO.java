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
    
    @JsonProperty("certificateId")
    private UUID certificateId;
    
    @JsonProperty("skillId")
    private UUID skillId; // certification skill
    
    @JsonProperty("proficiencyId")
    private UUID proficiencyId;
    
    @JsonProperty("issuedDate")
    private LocalDate issuedDate;
    
    @JsonProperty("expiryDate")
    private LocalDate expiryDate;
    
    @JsonProperty("certificateFile")
    private String certificateFile;
    
    @JsonProperty("activeFlag")
    private Boolean activeFlag;
}
