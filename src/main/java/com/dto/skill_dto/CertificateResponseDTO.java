package com.dto.skill_dto;

import com.entity_enums.skill_enums.CertificateType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class CertificateResponseDTO {
    private UUID certificateId;

    private String certificateName;

    private String providerName;

    private CertificateType certificateType;

    private UUID skillId;

    private String skillName;

    private String categoryName;

    private Boolean timeBound;

    private Integer validityMonths;

    private Boolean activeFlag;

    private LocalDateTime createdAt;
}
