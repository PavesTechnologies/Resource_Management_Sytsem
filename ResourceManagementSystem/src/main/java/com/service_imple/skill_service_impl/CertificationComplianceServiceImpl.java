package com.service_imple.skill_service_impl;

import com.dto.skill_dto.AllocationValidationRequestDTO;
import com.entity.skill_entities.ResourceSkill;
import com.global_exception_handler.CertificationComplianceException;
import com.repo.skill_repo.ResourceSkillRepository;
import com.service_interface.skill_service_interface.CertificationComplianceService;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Setter
@RequiredArgsConstructor
public class CertificationComplianceServiceImpl implements CertificationComplianceService {
    private final ResourceSkillRepository resourceSkillRepository;

    @Override
    public void validateCertificationCompliance(AllocationValidationRequestDTO dto) {
        for (UUID certSkillId : dto.getRequiredCertificationSkillIds()) {

            ResourceSkill cert = resourceSkillRepository
                    .findByResourceIdAndSkillIdAndActiveFlagTrue(
                            dto.getResourceId(), certSkillId)
                    .orElse(null);

            if (cert == null) {
                throw new CertificationComplianceException(
                        "Missing required certification");
            }

            if (cert.getExpiryDate() != null &&
                    cert.getExpiryDate().isBefore(LocalDate.now())) {

                throw new CertificationComplianceException(
                        "Certification expired for skill: " + certSkillId);
            }
        }
    }

}
