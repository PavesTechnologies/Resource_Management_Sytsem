package com.service_interface.skill_service_interface;

import com.dto.skill_dto.ResourceSkillBulkRequestDTO;
import com.dto.skill_dto.ResourceSkillProfileResponseDTO;
import com.dto.skill_dto.ResourceSkillRequestDTO;
import com.dto.skill_dto.ResourceSubSkillRequestDTO;
import com.entity.skill_entities.ResourceSkill;
import com.entity.skill_entities.ResourceSubSkill;

import java.util.List;
import java.util.UUID;

public interface ResourceSkillService {
    
    String addSkillsToResource(ResourceSkillBulkRequestDTO dto);
    
    String addSingleSkillToResource(ResourceSkillRequestDTO dto);
    
    String addSingleSubSkillToResource(ResourceSubSkillRequestDTO dto);
    
    List<ResourceSkillProfileResponseDTO> getResourceSkillProfile(Long resourceId);
    
    ResourceSkill updateResourceSkill(UUID resourceSkillId, ResourceSkillRequestDTO dto);
    
    ResourceSubSkill updateResourceSubSkill(UUID resourceSubSkillId, ResourceSubSkillRequestDTO dto);
}
