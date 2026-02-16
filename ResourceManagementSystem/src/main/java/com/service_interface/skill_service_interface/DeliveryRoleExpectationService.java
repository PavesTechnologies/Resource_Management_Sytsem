package com.service_interface.skill_service_interface;

import com.dto.skill_dto.DeliveryRoleExpectationRequest;
import com.dto.skill_dto.DeliveryRoleExpectationResponse;
import com.dto.skill_dto.RoleListResponse;

import java.util.List;

public interface DeliveryRoleExpectationService {

    DeliveryRoleExpectationResponse createOrUpdateRoleExpectations(DeliveryRoleExpectationRequest request);

    DeliveryRoleExpectationResponse getRoleExpectations(String roleName);

    List<DeliveryRoleExpectationResponse> getAllRoleExpectations();

    RoleListResponse getAvailableRoles();

    void deleteRoleExpectations(String roleName);

    boolean hasRoleExpectations(String roleName);
}
