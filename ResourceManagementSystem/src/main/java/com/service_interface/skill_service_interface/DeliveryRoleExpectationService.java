package com.service_interface.skill_service_interface;

import com.dto.ApiResponse;
import com.dto.skill_dto.DeliveryRoleExpectationRequest;
import com.dto.skill_dto.DeliveryRoleExpectationResponse;
import com.dto.skill_dto.RoleExpectationRequest;
import com.dto.skill_dto.RoleExpectationWithMandatoryResponse;
import com.dto.skill_dto.RoleListResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public interface DeliveryRoleExpectationService {

    ResponseEntity<ApiResponse<String>> saveOrUpdateRoleExpectations(RoleExpectationRequest request);

    DeliveryRoleExpectationResponse createOrUpdateRoleExpectations(DeliveryRoleExpectationRequest request);

    DeliveryRoleExpectationResponse getRoleExpectations(String roleName);

    RoleExpectationWithMandatoryResponse getRoleExpectationsWithMandatory(String roleName);

    List<DeliveryRoleExpectationResponse> getAllRoleExpectations();

    RoleListResponse getAvailableRoles();

    void deleteRoleExpectations(String roleName);

    boolean hasRoleExpectations(String roleName);

    boolean isResourceEligibleForRole(String roleName, List<UUID> resourceSkillIds);
}
