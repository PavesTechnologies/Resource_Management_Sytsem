package com.service_interface.skill_service_interface;

import com.dto.skill_dto.ResourceSkillBulkRequestDTO;
import com.dto.skill_dto.ResourceSkillProfileResponseDTO;
import com.dto.skill_dto.ResourceSkillRequestDTO;
import com.dto.skill_dto.ResourceSubSkillRequestDTO;

import java.util.List;

public interface ResourceSkillService {
    
    String addSkillsToResource(ResourceSkillBulkRequestDTO dto);
    
    String addSingleSkillToResource(ResourceSkillRequestDTO dto);
    
    String addSingleSubSkillToResource(ResourceSubSkillRequestDTO dto);
    
    List<ResourceSkillProfileResponseDTO> getResourceSkillProfile(Long resourceId);
}
