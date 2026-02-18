package com.service_imple.skill_service_impl;

import com.dto.skill_dto.CertificateRequestDTO;
import com.entity.skill_entities.Certificate;
import com.entity.skill_entities.Skill;
import com.entity_enums.skill_enums.CertificateType;
import com.global_exception_handler.CertificationComplianceException;
import com.repo.skill_repo.CertificateRepository;
import com.repo.skill_repo.SkillRepository;
import com.service_interface.skill_service_interface.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {
    private final CertificateRepository certificateRepository;
    private final SkillRepository skillRepository;

    @Override
    @Transactional
    public String CreateCertificate(CertificateRequestDTO dto) {

        if (dto.getCertificateType() == null) {
            throw new CertificationComplianceException("Certificate type is required");
        }

        if (dto.getCertificateType() == CertificateType.SKILL_BASED) {

            if (dto.getSkillId() == null) {
                throw new CertificationComplianceException(
                        "Skill ID is required for skill-based certification");
            }

            Skill skill = skillRepository.findById(dto.getSkillId())
                    .orElseThrow(() ->
                            new CertificationComplianceException("Skill not found"));

//            if (!"CERTIFICATION".equalsIgnoreCase(skill.getSkillType())) {
//                throw new CertificationComplianceException(
//                        "Skill is not marked as CERTIFICATION type");
//            }

//        } else if (dto.getCertificateType() == CertificateType.ACHIEVEMENT) {

            if (dto.getSkillId() != null) {
                throw new CertificationComplianceException(
                        "Achievement certificate must not be linked to a skill");
            }
        }

        Certificate certificate = Certificate.builder()
                .certificateId(UUID.randomUUID())
                .skillId(dto.getSkillId())
//                .certificateType(dto.getCertificateType())
                .providerName(dto.getProviderName())
                .certifiedAt(dto.getCertifiedAt())
                .expiryDate(dto.getExpiryDate())
                .activeFlag(true)
                .build();

        certificateRepository.save(certificate);

        return "Certificate created successfully";
    }

    @Override
    public List<Skill> getAllCertificationSkills() {
        return skillRepository.findBySkillTypeIgnoreCaseAndStatusIgnoreCase("CERTIFICATION", "ACTIVE");
    }

    @Override
    public Skill getCertificationSkillById(UUID id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new CertificationComplianceException("Certification skill not found"));
    }

    @Override
    public List<Skill> getCertificationSkillsByCategory(UUID categoryId) {
        return skillRepository.findBySkillTypeIgnoreCaseAndStatusIgnoreCaseAndCategory_Id("CERTIFICATION", "ACTIVE", categoryId);
    }

}
