package com.service_imple.skill_service_impl;


import com.dto.skill_dto.ResourceCertificateRequestDTO;
import com.entity.skill_entities.Certificate;
import com.entity.skill_entities.ResourceCertificate;
import com.entity.skill_entities.ResourceSkill;
import com.entity.skill_entities.Skill;
import com.entity.resource_entities.Resource;
import com.entity_enums.skill_enums.CertificateStatus;
import com.global_exception_handler.CertificationComplianceException;
import com.repo.skill_repo.CertificateRepository;
import com.repo.skill_repo.ResourceCertificateRepository;
import com.repo.skill_repo.SkillRepository;
import com.repo.resource_repo.ResourceRepository;
import com.service_interface.skill_service_interface.ResourceCertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourceCertificateServiceImpl implements ResourceCertificateService {
    private final ResourceCertificateRepository resourceCertificateRepository;
    private final CertificateRepository certificateRepository;
    private final SkillRepository skillRepository;
    private final ResourceRepository resourceRepository;

    @Override
    @Transactional
    public String assignCertificate(ResourceCertificateRequestDTO dto) {
        // Validate resource exists and is active before proceeding
        validateResourceExistsAndActive(dto.getResourceId());

        Certificate master = certificateRepository.findById(dto.getCertificateId())
                .orElseThrow(() ->
                        new CertificationComplianceException("Certificate master not found"));

        if (dto.getIssuedDate() == null) {
            throw new CertificationComplianceException("Issued date required");
        }

        LocalDate expiryDate = null;
        CertificateStatus status;

        if (Boolean.TRUE.equals(master.getTimeBound())) {

            expiryDate = dto.getIssuedDate()
                    .plusMonths(master.getValidityMonths());

            status = calculateStatus(expiryDate);

        } else {
            status = CertificateStatus.ACTIVE;
        }

        ResourceCertificate entity = ResourceCertificate.builder()
                .resourceId(dto.getResourceId())
                .certificateId(dto.getCertificateId())
                .issuedDate(dto.getIssuedDate())
                .expiryDate(expiryDate)
                .status(status)
                .activeFlag(true)
                .build();

        resourceCertificateRepository.save(entity);

        return "Certificate assigned successfully";
    }

    private CertificateStatus calculateStatus(LocalDate expiryDate) {

        LocalDate today = LocalDate.now();

        if (expiryDate.isBefore(today)) {
            return CertificateStatus.EXPIRED;
        }

        if (expiryDate.isBefore(today.plusDays(30))) {
            return CertificateStatus.EXPIRING_SOON;
        }

        return CertificateStatus.ACTIVE;
    }

    @Transactional
    public String renewCertificate(UUID resourceCertificateId,
                                   LocalDate newIssuedDate) {

        ResourceCertificate rc = resourceCertificateRepository
                .findById(resourceCertificateId)
                .orElseThrow(() ->
                        new CertificationComplianceException("Certificate not found"));

        Certificate master = certificateRepository
                .findById(rc.getCertificateId())
                .orElseThrow(() ->
                        new CertificationComplianceException("Master not found"));

        if (Boolean.TRUE.equals(master.getTimeBound())) {

            LocalDate newExpiry =
                    newIssuedDate.plusMonths(master.getValidityMonths());

            rc.setIssuedDate(newIssuedDate);
            rc.setExpiryDate(newExpiry);
            rc.setStatus(calculateStatus(newExpiry));
        }

        return "Certificate renewed successfully";
    }

    @Override
    public List<ResourceCertificate> getCertificatesByResourceId(Long resourceId) {
        return resourceCertificateRepository.findByResourceIdAndActiveFlagTrue(resourceId);
    }

    @Override
    public List<ResourceCertificate> getAllCertificates() {
        return resourceCertificateRepository.findByActiveFlagTrue();
    }

    @Override
    public ResourceCertificate getCertificateById(UUID id) {
        return resourceCertificateRepository.findById(id)
                .orElseThrow(() -> new CertificationComplianceException("Certificate not found"));
    }

    /**
     * Validates that a resource exists and is active before allowing certificate assignments
     * @param resourceId The resource ID to validate
     * @throws CertificationComplianceException if resource doesn't exist or is not active
     */
    private void validateResourceExistsAndActive(Long resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new CertificationComplianceException(
                        "Resource not found with ID: " + resourceId));
        
        if (!Boolean.TRUE.equals(resource.getActiveFlag())) {
            throw new CertificationComplianceException(
                    "Resource is not active: " + resource.getFullName() + " (ID: " + resourceId + ")");
        }
    }

}
