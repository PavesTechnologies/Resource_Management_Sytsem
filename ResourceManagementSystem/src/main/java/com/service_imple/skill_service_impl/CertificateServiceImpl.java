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

        if (dto.getTimeBound() == null) {
            throw new CertificationComplianceException("timeBound flag required");
        }

        if (dto.getTimeBound() && dto.getValidityMonths() == null) {
            throw new CertificationComplianceException(
                    "Validity months required for time-bound certificates");
        }

        Certificate certificate = Certificate.builder()
                .certificateId(UUID.randomUUID())
                .skillId(dto.getSkillId())
                .providerName(dto.getProviderName())
                .timeBound(dto.getTimeBound())
                .validityMonths(dto.getValidityMonths())
                .activeFlag(true)
                .build();

        certificateRepository.save(certificate);

        return "Certificate master created successfully";
    }

    @Override
    public List<Certificate> getAllCertificationSkills() {
        return certificateRepository.findAll();
        //return skillRepository.findBySkillTypeIgnoreCaseAndStatusIgnoreCase("CERTIFICATION", "ACTIVE");
    }

    @Override
    public Skill getCertificationSkillById(UUID id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new CertificationComplianceException("Certification skill not found"));
    }

    @Override
    public List<Skill> getCertificationSkillsByCategory(UUID categoryId) {
        //return skillRepository.findBySkillTypeIgnoreCaseAndStatusIgnoreCaseAndCategory_Id("CERTIFICATION", "ACTIVE", categoryId);
        return null;
    }

}
