package com.service_interface.skill_service_interface;

import com.dto.skill_dto.EmployeeSkillRequestDto;
import com.dto.skill_dto.EmployeeSkillResponseDto;
import com.dto.skill_dto.EmployeeSkillsRequestDto;

import java.util.List;

public interface EmployeeSkillMappingService {

    String saveSkillMappings(EmployeeSkillsRequestDto requestDto);

    String saveSkillMapping(EmployeeSkillRequestDto requestDto);

    List<EmployeeSkillResponseDto> getEmployeeSkills(String resourceId);
}
