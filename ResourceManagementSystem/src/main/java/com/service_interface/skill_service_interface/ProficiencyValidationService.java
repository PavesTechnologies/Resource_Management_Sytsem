package com.service_interface.skill_service_interface;

import java.util.UUID;

public interface ProficiencyValidationService {
    void validateProficiency(Long resourceId,
                             UUID skillId,
                             UUID requiredProficiencyId);
}

