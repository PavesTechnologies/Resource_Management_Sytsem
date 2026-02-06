package com.service_imple.project_service_impl;


import com.global_exception_handler.ProjectExceptionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffingService {
    private final ProjectDemandValidationService projectValidator;

//    public void initiateStaffing(Long pmsProjectId, UUID rmsUserId) {
//
//        // Optional: authorization check
//        if (!isResourceManager(rmsUserId)) {
//            throw new ProjectExceptionHandler(
//                    HttpStatus.FORBIDDEN,
//                    "ACCESS_DENIED",
//                    "Only Resource Managers can initiate staffing"
//            );
//        }
//
//        // 🔐 STORY 7 + 8 enforced here
//        projectValidator.validateProjectForStaffing(pmsProjectId);
//
//        // Continue staffing logic
//    }

//    private boolean isResourceManager(UUID userId) {
//        // your RMS role logic
//        return true;
//    }
}
