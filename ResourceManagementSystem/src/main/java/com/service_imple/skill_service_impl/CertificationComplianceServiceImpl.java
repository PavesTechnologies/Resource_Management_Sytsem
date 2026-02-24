package com.service_imple.skill_service_impl;

import com.dto.skill_dto.AllocationValidationRequestDTO;
import com.entity.skill_entities.ResourceCertificate;
import com.entity.skill_entities.ResourceSkill;
import com.entity_enums.skill_enums.CertificateStatus;
import com.global_exception_handler.CertificationComplianceException;
import com.repo.skill_repo.ResourceCertificateRepository;
import com.repo.skill_repo.ResourceSkillRepository;
import com.service_interface.skill_service_interface.CertificationComplianceService;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CertificationComplianceServiceImpl implements CertificationComplianceService {
    private final ResourceCertificateRepository resourceCertificateRepository;

    @Override
    public void validateCertificationCompliance(
            AllocationValidationRequestDTO dto) {

        if (dto.getRequiredCertificationSkillIds() == null
                || dto.getRequiredCertificationSkillIds().isEmpty()) {
            throw new CertificationComplianceException(
                    "Required certification list cannot be empty");
        }

        for (UUID certId : dto.getRequiredCertificationSkillIds()) {

            ResourceCertificate rc =
                    resourceCertificateRepository
                            .findByResourceIdAndCertificateIdAndActiveFlagTrue(
                                    dto.getResourceId(), certId)
                            .orElseThrow(() ->
                                    new CertificationComplianceException(
                                            "Missing required certification"));

            if (rc.getStatus() == CertificateStatus.EXPIRED) {
                throw new CertificationComplianceException(
                        "Certificate expired on " + rc.getExpiryDate());
            }
        }
    }
}
