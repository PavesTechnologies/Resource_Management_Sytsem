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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
    public String assignCertificate(ResourceCertificateRequestDTO dto, MultipartFile certificateFile) {

        // ✅ 1. Validate resource
        validateResourceExistsAndActive(dto.getResourceId());

        // ✅ 2. Validate certificate
        Certificate master = certificateRepository.findById(dto.getCertificateId())
                .orElseThrow(() ->
                        new CertificationComplianceException("Certificate master not found"));

        if (dto.getIssuedDate() == null) {
            throw new CertificationComplianceException("Issued date required");
        }

        // ✅ 3. 🔥 ADD DUPLICATE CHECK HERE
        Optional<ResourceCertificate> existing =
                resourceCertificateRepository
                        .findByResourceIdAndCertificateIdAndActiveFlagTrue(
                                dto.getResourceId(),
                                dto.getCertificateId()
                        );

        if (existing.isPresent()) {
            throw new CertificationComplianceException(
                    "Certificate already assigned to this resource"
            );
        }

        // ✅ 4. Expiry logic
        LocalDate expiryDate = null;
        CertificateStatus status;

        if (Boolean.TRUE.equals(master.getTimeBound())) {
            if (dto.getExpiryDate() != null) {
                expiryDate = dto.getExpiryDate();
            } else {
                expiryDate = dto.getIssuedDate()
                        .plusMonths(master.getValidityMonths());
            }

            status = calculateStatus(expiryDate);

        } else {
            status = CertificateStatus.ACTIVE;
        }

        // ✅ 5. File handling
        byte[] fileBytes = null;
        String fileName = null;
        String fileType = null;

        if (certificateFile != null && !certificateFile.isEmpty()) {
            try {
                fileBytes = certificateFile.getBytes();
                fileName = certificateFile.getOriginalFilename();
                fileType = certificateFile.getContentType();
            } catch (IOException e) {
                throw new CertificationComplianceException("Failed to process certificate file: " + e.getMessage());
            }
        }

        // ✅ 6. Save entity
        ResourceCertificate entity = ResourceCertificate.builder()
                .resourceId(dto.getResourceId())
                .certificateId(dto.getCertificateId())
                .issuedDate(dto.getIssuedDate())
                .certificateFile(fileBytes)
                .fileName(fileName)
                .fileType(fileType)
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

    @Override
    @Transactional
    public ResourceCertificate updateResourceCertificate(UUID resourceCertificateId, ResourceCertificateRequestDTO dto, MultipartFile certificateFile) {
        ResourceCertificate existingResourceCertificate = resourceCertificateRepository.findById(resourceCertificateId)
                .orElseThrow(() -> new CertificationComplianceException(
                        "Resource certificate not found with ID: " + resourceCertificateId));

        // Validate resource exists and is active if changing resource
        if (!existingResourceCertificate.getResourceId().equals(dto.getResourceId())) {
            validateResourceExistsAndActive(dto.getResourceId());
        }

        // Validate certificate exists if changing certificate
        if (!existingResourceCertificate.getCertificateId().equals(dto.getCertificateId())) {
            Certificate master = certificateRepository.findById(dto.getCertificateId())
                    .orElseThrow(() -> new CertificationComplianceException("Certificate master not found"));
        }

        // Check for duplicate assignment if changing resource or certificate
        if (!existingResourceCertificate.getResourceId().equals(dto.getResourceId()) || 
            !existingResourceCertificate.getCertificateId().equals(dto.getCertificateId())) {
            
            Optional<ResourceCertificate> existing = resourceCertificateRepository
                    .findByResourceIdAndCertificateIdAndActiveFlagTrue(dto.getResourceId(), dto.getCertificateId());
            
            if (existing.isPresent() && !existing.get().getId().equals(resourceCertificateId)) {
                throw new CertificationComplianceException(
                        "Certificate already assigned to this resource");
            }
        }

        // Update fields
        existingResourceCertificate.setResourceId(dto.getResourceId());
        existingResourceCertificate.setCertificateId(dto.getCertificateId());
        
        if (dto.getIssuedDate() != null) {
            existingResourceCertificate.setIssuedDate(dto.getIssuedDate());
            
            // Recalculate expiry date and status if time-bound certificate
            Certificate master = certificateRepository.findById(existingResourceCertificate.getCertificateId())
                    .orElseThrow(() -> new CertificationComplianceException("Certificate master not found"));
            
            if (Boolean.TRUE.equals(master.getTimeBound())) {
                // Use manual expiry date if provided, otherwise calculate automatically
                LocalDate newExpiryDate;
                if (dto.getExpiryDate() != null) {
                    newExpiryDate = dto.getExpiryDate();
                } else {
                    newExpiryDate = dto.getIssuedDate().plusMonths(master.getValidityMonths());
                }
                existingResourceCertificate.setExpiryDate(newExpiryDate);
                existingResourceCertificate.setStatus(calculateStatus(newExpiryDate));
            }
        } else if (dto.getExpiryDate() != null) {
            // Handle case where only expiry date is updated
            existingResourceCertificate.setExpiryDate(dto.getExpiryDate());
            existingResourceCertificate.setStatus(calculateStatus(dto.getExpiryDate()));
        }
        
        // Handle file update
        if (certificateFile != null && !certificateFile.isEmpty()) {
            try {
                existingResourceCertificate.setCertificateFile(certificateFile.getBytes());
                existingResourceCertificate.setFileName(certificateFile.getOriginalFilename());
                existingResourceCertificate.setFileType(certificateFile.getContentType());
            } catch (IOException e) {
                throw new CertificationComplianceException("Failed to process certificate file: " + e.getMessage());
            }
        }

        return resourceCertificateRepository.save(existingResourceCertificate);
    }

    @Override
    @Transactional
    public String deleteResourceCertificate(UUID resourceCertificateId) {
        ResourceCertificate resourceCertificate = resourceCertificateRepository.findById(resourceCertificateId)
                .orElseThrow(() -> new CertificationComplianceException(
                        "Resource certificate not found with ID: " + resourceCertificateId));
        
        // Soft delete by setting activeFlag to false
        resourceCertificate.setActiveFlag(false);
        resourceCertificateRepository.save(resourceCertificate);
        
        return "Resource certificate deleted successfully";
    }

}
