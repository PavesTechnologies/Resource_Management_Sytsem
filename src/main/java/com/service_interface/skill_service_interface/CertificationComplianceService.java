package com.service_interface.skill_service_interface;

import com.dto.skill_dto.AllocationValidationRequestDTO;

public interface CertificationComplianceService {
    void validateCertificationCompliance(AllocationValidationRequestDTO dto);
}
