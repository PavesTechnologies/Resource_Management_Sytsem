package com.service_interface.skill_service_interface;

import com.dto.skill_dto.ResourceCertificateRequestDTO;
import com.entity.skill_entities.ResourceSkill;

import java.util.List;
import java.util.UUID;

public interface ResourceCertificateService {
    String assignCertificate(ResourceCertificateRequestDTO dto);

    List<ResourceSkill> getCertificatesByResourceId(Long resourceId);
    
    List<ResourceSkill> getAllCertificates();
    
    ResourceSkill getCertificateById(UUID id);
}
