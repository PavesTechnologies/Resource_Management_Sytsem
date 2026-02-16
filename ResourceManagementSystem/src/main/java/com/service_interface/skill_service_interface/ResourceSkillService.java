package com.service_interface.skill_service_interface;

import com.dto.skill_dto.ResourceSkillBulkRequestDTO;
import com.dto.skill_dto.ResourceSkillProfileDTO;

import java.util.List;
import java.util.UUID;

public interface ResourceSkillService {
    
    String addSkillsToResource(ResourceSkillBulkRequestDTO dto);
    
    List<ResourceSkillProfileDTO> getResourceSkillProfile(UUID resourceId);
}
