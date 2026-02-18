package com.service_imple.skill_service_impl;


import com.dto.skill_dto.ResourceCertificateRequestDTO;
import com.entity.skill_entities.ResourceSkill;
import com.entity.skill_entities.Skill;
import com.global_exception_handler.CertificationComplianceException;
import com.repo.skill_repo.ResourceSkillRepository;
import com.repo.skill_repo.SkillRepository;
import com.service_interface.skill_service_interface.ResourceCertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourceCertificateServiceImpl implements ResourceCertificateService {
    private final ResourceSkillRepository resourceSkillRepository;
    private final SkillRepository skillRepository;

    @Override
    @Transactional
    public String assignCertificate(ResourceCertificateRequestDTO dto) {
        if (dto.getSkillId() == null) {
            throw new CertificationComplianceException("Skill ID is required");
        }
        if (dto.getResourceId() == null) {
            throw new CertificationComplianceException("Resource ID is required");
        }
        if (dto.getProficiencyId() == null) {
            throw new CertificationComplianceException("Proficiency ID is required");
        }

        Skill skill = skillRepository.findById(dto.getSkillId())
                .orElseThrow(() ->
                        new CertificationComplianceException("Certification skill not found"));

        if (!"CERTIFICATION".equalsIgnoreCase(skill.getSkillType())) {
            throw new CertificationComplianceException(
                    "Skill is not a certification");
        }

        ResourceSkill entity = ResourceSkill.builder()
                .resourceId(dto.getResourceId())
                .skillId(dto.getSkillId())
                .proficiencyId(dto.getProficiencyId())
                .expiryDate(dto.getExpiryDate())
                .activeFlag(true)
                .build();

        resourceSkillRepository.save(entity);

        return "Certification assigned to resource";
    }

    @Override
    public List<ResourceSkill> getCertificatesByResourceId(Long resourceId) {
        return resourceSkillRepository.findByResourceIdAndActiveFlagTrue(resourceId);
    }

    @Override
    public List<ResourceSkill> getAllCertificates() {
        return resourceSkillRepository.findByActiveFlagTrue();
    }

    @Override
    public ResourceSkill getCertificateById(UUID id) {
        return resourceSkillRepository.findById(id)
                .orElseThrow(() -> new CertificationComplianceException("Certificate not found"));
    }

}
