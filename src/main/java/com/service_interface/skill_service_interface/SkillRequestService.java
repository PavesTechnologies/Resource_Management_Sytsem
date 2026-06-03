package com.service_interface.skill_service_interface;

import com.dto.skill_dto.BulkSkillApprovalRequestDto;
import com.dto.skill_dto.NewSkillRequestDto;
import com.dto.skill_dto.SkillApprovalDto;
import com.dto.skill_dto.SkillRequestResponseDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SkillRequestService {

    String submitSkillRequest(NewSkillRequestDto requestDto);

    List<SkillRequestResponseDto> getAllSkillRequests();

    List<SkillRequestResponseDto> getRequestsByResourceId(String resourceId);

    String approveSkillRequest(UUID requestId, String approvedBy);

    String rejectSkillRequest(UUID requestId, SkillApprovalDto approvalDto);

    Map<String, Integer> bulkApproveByEmployee(String resourceId, BulkSkillApprovalRequestDto dto);

    Map<String, Integer> bulkRejectByEmployee(String resourceId, BulkSkillApprovalRequestDto dto);
}
