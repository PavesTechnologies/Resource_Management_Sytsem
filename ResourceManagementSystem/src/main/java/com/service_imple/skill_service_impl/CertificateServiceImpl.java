package com.service_imple.skill_service_impl;

import com.dto.UserDTO;
import com.entity.skill_entities.Certificate;
import com.repo.skill_repo.CertificateRepository;
import com.service_interface.skill_service_interface.CertificateService;
import com.service_interface.skill_service_interface.SkillAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepo;
    private final SkillAuditService auditService;

    @Override
    public Certificate assignCertificate(UUID skillId,
                                         String certificateId,
                                         String providerName,
                                         String certificateFile,
                                         LocalDate certifiedAt,
                                         LocalDate expiryDate,
                                         UserDTO user) {

        Certificate cert = Certificate.builder()
                .certificateId(UUID.randomUUID()) // Generate UUID for certificate ID
                .skillId(skillId)
                .providerName(providerName)
                .certificateFile(certificateFile)
                .certifiedAt(certifiedAt)
                .expiryDate(expiryDate)
                .activeFlag(true)
                .build();

        Certificate saved = certificateRepo.save(cert);

        auditService.auditCreate(
                "CERTIFICATE",
                saved.getCertificateId().toString(),
                saved,
                user.getEmail()
        );

        return saved;
    }

    @Override
    public Optional<Certificate> getActiveCertificate(UUID skillId) {
        return certificateRepo.findBySkillIdAndActiveFlagTrue(skillId);
    }
}

