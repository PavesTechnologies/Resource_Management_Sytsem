package com.service_imple.skill_service_impl;

import com.dto.skill_dto.CertificateRequestDTO;
import com.dto.skill_dto.CertificateResponseDTO;
import com.entity.skill_entities.Certificate;
import com.entity.skill_entities.Skill;
import com.entity_enums.skill_enums.CertificateType;
import com.global_exception_handler.SkillExceptionHandler;
import com.repo.skill_repo.CertificateRepository;
import com.repo.skill_repo.ResourceCertificateRepository;
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
    private final ResourceCertificateRepository resourceCertificateRepository;

    @Override
    @Transactional
    public String CreateCertificate(CertificateRequestDTO dto) {

        if (Boolean.TRUE.equals(dto.getTimeBound()) && dto.getValidityMonths() == null) {
            throw SkillExceptionHandler.badRequest(
                    "Validity months required for time-bound certificates");
        }
        if (dto.getCertificateType() == null) {
            throw SkillExceptionHandler.badRequest("Certificate type is required");
        }

        if (dto.getCertificateName() == null || dto.getCertificateName().trim().isEmpty()) {
            throw SkillExceptionHandler.badRequest("Certificate name is required");
        }
        if (dto.getCertificateType() == CertificateType.SKILL_BASED
                && dto.getSkillId() == null) {

            throw SkillExceptionHandler.badRequest(
                    "Skill is required for skill-based certificates");
        }

        Certificate certificate = Certificate.builder()
                .certificateId(UUID.randomUUID())
                .certificateType(dto.getCertificateType())
                .skillId(dto.getSkillId())
                .providerName(dto.getProviderName())
                .certificateName(dto.getCertificateName())
                .timeBound(dto.getTimeBound() != null ? dto.getTimeBound() : false)
                .validityMonths(dto.getValidityMonths())
                .activeFlag(true)
                .build();

        certificateRepository.save(certificate);

        return "Certificate master created successfully";
    }

    @Override
    public List<CertificateResponseDTO> getAllCertificationSkills() {

        List<Certificate> certificates = certificateRepository.findByActiveFlagTrue();

        return certificates.stream().map(certificate -> {

            Skill skill = null;

            if (certificate.getSkillId() != null) {
                skill = skillRepository.findById(certificate.getSkillId()).orElse(null);
            }

            return CertificateResponseDTO.builder()
                    .certificateId(certificate.getCertificateId())
                    .certificateName(certificate.getCertificateName())
                    .providerName(certificate.getProviderName())
                    .certificateType(certificate.getCertificateType())
                    .skillId(certificate.getSkillId())
                    .skillName(
                            skill != null ? skill.getName() : null
                    )
                    .categoryName(
                            skill != null && skill.getCategory() != null
                                    ? skill.getCategory().getName()
                                    : null
                    )
                    .timeBound(certificate.getTimeBound())
                    .validityMonths(certificate.getValidityMonths())
                    .activeFlag(certificate.getActiveFlag())
                    .build();

        }).toList();
    }

    @Override
    public Skill getCertificationSkillById(UUID id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> SkillExceptionHandler.badRequest("Certification skill not found"));
    }

    @Override
    public List<Skill> getCertificationSkillsByCategory(UUID categoryId) {
        //return skillRepository.findBySkillTypeIgnoreCaseAndStatusIgnoreCaseAndCategory_Id("CERTIFICATION", "ACTIVE", categoryId);
        return null;
    }

    @Override
    @Transactional
    public Certificate updateCertificate(UUID certificateId, CertificateRequestDTO dto) {
        Certificate existingCertificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> SkillExceptionHandler.badRequest("Certificate not found with ID: " + certificateId));

        if (Boolean.TRUE.equals(dto.getTimeBound()) && dto.getValidityMonths() == null) {
            throw SkillExceptionHandler.badRequest(
                    "Validity months required for time-bound certificates");
        }

        // Update fields
        if (dto.getCertificateType() != null) {
            existingCertificate.setCertificateType(dto.getCertificateType());
        }
        if (dto.getSkillId() != null) {
            existingCertificate.setSkillId(dto.getSkillId());
        }
        if (dto.getProviderName() != null) {
            existingCertificate.setProviderName(dto.getProviderName());
        }
        if (dto.getCertificateName() != null) {
            existingCertificate.setCertificateName(dto.getCertificateName());
        }
        if (dto.getTimeBound() != null) {
            existingCertificate.setTimeBound(dto.getTimeBound());
        }
        if (dto.getValidityMonths() != null) {
            existingCertificate.setValidityMonths(dto.getValidityMonths());
        }
        if (dto.getActiveFlag() != null) {
            existingCertificate.setActiveFlag(dto.getActiveFlag());
        }

        return certificateRepository.save(existingCertificate);
    }

    @Override
    @Transactional
    public void deleteCertificate(UUID id) {

        Certificate existingCertificate = certificateRepository.findById(id)
                .orElseThrow(() ->
                        SkillExceptionHandler.badRequest(
                                "Certificate not found with ID: " + id));

        if (Boolean.FALSE.equals(existingCertificate.getActiveFlag())) {
            throw SkillExceptionHandler.badRequest(
                    "Certificate is already deleted (inactive)");
        }

        boolean assigned =
                resourceCertificateRepository
                        .existsByCertificateIdAndActiveFlagTrue(id);

        if (assigned) {
            throw SkillExceptionHandler.badRequest(
                    "Certificate cannot be deleted because it is assigned to one or more resources");
        }

        existingCertificate.setActiveFlag(false);

        certificateRepository.save(existingCertificate);
    }
}