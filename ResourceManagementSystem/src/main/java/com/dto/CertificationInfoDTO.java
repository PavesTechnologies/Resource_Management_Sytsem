package com.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificationInfoDTO {
    
    private String certificateName;
    private String providerName;
    private LocalDate expiryDate;
    private Boolean isActive;
}
