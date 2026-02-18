package com.service_interface.skill_service_interface;

import com.dto.skill_dto.CertificateRequestDTO;
import com.entity.skill_entities.Skill;

import java.util.List;
import java.util.UUID;

public interface CertificateService {
    String CreateCertificate(CertificateRequestDTO dto);
    
    List<Skill> getAllCertificationSkills();
    
    Skill getCertificationSkillById(UUID id);
    
    List<Skill> getCertificationSkillsByCategory(UUID categoryId);
}
