package com.service_interface.skill_service_interface;

import com.dto.skill_dto.ResourceSkillBulkRequestDTO;
import com.dto.skill_dto.ResourceSkillProfileResponseDTO;

import java.util.List;

public interface ResourceSkillService {
    
    String addSkillsToResource(ResourceSkillBulkRequestDTO dto);
    
    List<ResourceSkillProfileResponseDTO> getResourceSkillProfile(Long resourceId);
}
