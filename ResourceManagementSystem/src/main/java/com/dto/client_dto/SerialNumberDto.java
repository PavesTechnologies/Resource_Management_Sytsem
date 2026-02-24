package com.dto.client_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for serial number response in dropdown.
 * Simplified response for frontend consumption.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SerialNumberDto {
    
    private String serialNumber;
    private String status;
    
    /**
     * Factory method to create DTO from entity.
     * 
     * @param serialNumber ClientAssetSerial entity
     * @return SerialNumberDto
     */
    public static SerialNumberDto fromEntity(com.entity.client_entities.ClientAssetSerial serialNumber) {
        return SerialNumberDto.builder()
                .serialNumber(serialNumber.getSerialNumber())
                .status(serialNumber.getStatus().name())
                .build();
    }
}
