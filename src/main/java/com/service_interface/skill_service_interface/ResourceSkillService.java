package com.service_interface.skill_service_interface;

import com.dto.skill_dto.EmployeeSkillRequestDto;
import com.dto.skill_dto.EmployeeSkillResponseDto;
import com.dto.skill_dto.EmployeeSkillsRequestDto;
import com.dto.skill_dto.ResourceSkillBulkRequestDTO;
import com.dto.skill_dto.ResourceSkillProfileResponseDTO;
import com.dto.skill_dto.ResourceSkillRequestDTO;
import com.dto.skill_dto.ResourceSkillResponseDTO;
import com.dto.skill_dto.ResourceSubSkillRequestDTO;
import com.entity.skill_entities.ResourceSkill;
import com.entity.skill_entities.ResourceSubSkill;

import java.util.List;
import java.util.UUID;

public interface ResourceSkillService {
    
    String saveSkillMappings(EmployeeSkillsRequestDto requestDto);

    String saveSkillMapping(EmployeeSkillRequestDto requestDto);

    List<EmployeeSkillResponseDto> getEmployeeSkills(String resourceId);

    String addSkillsToResource(ResourceSkillBulkRequestDTO dto);
    
    String addSingleSkillToResource(ResourceSkillRequestDTO dto);
    
    String addSingleSubSkillToResource(ResourceSubSkillRequestDTO dto);
    
    List<ResourceSkillProfileResponseDTO> getResourceSkillProfile(String resourceId);
    
    List<ResourceSkillResponseDTO> getAllResourceSkills(String resourceId);
    
    List<ResourceSubSkill> getAllResourceSubSkills(String resourceId);
    
    List<ResourceSkill> getAllResourceSkills();
    
    List<ResourceSubSkill> getAllResourceSubSkills();
    
    ResourceSkill getResourceSkillById(UUID resourceSkillId);
    
    ResourceSubSkill getResourceSubSkillById(UUID resourceSubSkillId);
    
    ResourceSkill updateResourceSkill(UUID resourceSkillId, ResourceSkillRequestDTO dto);
    
    String deleteResourceSkill(UUID resourceSkillId);
    
    String deleteResourceSubSkill(UUID resourceSubSkillId);
}
