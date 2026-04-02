package com.dto.skill_dto;


import com.entity_enums.skill_enums.CertificateType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class CertificateRequestDTO {
    private UUID skillId;
    private String providerName;
    private String certificateName;
//    private CertificateType certificateType;
//    private LocalDate certifiedAt;
//    private LocalDate expiryDate;
    private Integer validityMonths;
    private Boolean timeBound;
    private Boolean activeFlag;
}
