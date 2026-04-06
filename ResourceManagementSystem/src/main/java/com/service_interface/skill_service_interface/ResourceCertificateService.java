package com.service_interface.skill_service_interface;

import com.dto.skill_dto.ResourceCertificateRequestDTO;
import com.entity.skill_entities.ResourceCertificate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ResourceCertificateService {
    String assignCertificate(ResourceCertificateRequestDTO dto, MultipartFile certificateFile);

    List<ResourceCertificate> getCertificatesByResourceId(Long resourceId);
    
    List<ResourceCertificate> getAllCertificates();
    
    ResourceCertificate getCertificateById(UUID id);
    
    ResourceCertificate updateResourceCertificate(UUID resourceCertificateId, ResourceCertificateRequestDTO dto, MultipartFile certificateFile);
}
