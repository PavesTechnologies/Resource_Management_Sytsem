package com.controller.cdc_controllers;

import com.cdc.service.EosDirectResyncService;
import com.dto.centralised_dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cdc/resync")
@CrossOrigin
@RequiredArgsConstructor
@Slf4j
public class CdcAdminController {

    private final EosDirectResyncService eosDirectResyncService;

    @PostMapping("/eos/offer-letter")
    @PreAuthorize("hasAnyRole('Admin','Resource_Manager')")
    public ResponseEntity<ApiResponse<String>> resyncOfferLetter(@RequestParam String employeeId) {
        try {
            eosDirectResyncService.resync("EOS-offer_letter_details", employeeId);
            return ResponseEntity.ok(ApiResponse.success(
                    "Offer letter re-sync triggered for employeeId: " + employeeId, employeeId));
        } catch (Exception e) {
            log.error("Failed to re-sync offer letter for employeeId={}: {}", employeeId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Re-sync failed: " + e.getMessage()));
        }
    }

    @PostMapping("/eos/employee")
    @PreAuthorize("hasAnyRole('Admin','Resource_Manager')")
    public ResponseEntity<ApiResponse<String>> resyncEmployee(@RequestParam String employeeId) {
        try {
            eosDirectResyncService.resync("EOS-employee_details", employeeId);
            return ResponseEntity.ok(ApiResponse.success(
                    "Employee details re-sync triggered for employeeId: " + employeeId, employeeId));
        } catch (Exception e) {
            log.error("Failed to re-sync employee details for employeeId={}: {}", employeeId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Re-sync failed: " + e.getMessage()));
        }
    }

    @PostMapping("/eos/exit")
    @PreAuthorize("hasAnyRole('Admin','Resource_Manager')")
    public ResponseEntity<ApiResponse<String>> resyncEmployeeExit(@RequestParam String employeeId) {
        try {
            eosDirectResyncService.resync("EOS-employee_exit", employeeId);
            return ResponseEntity.ok(ApiResponse.success(
                    "Employee exit re-sync triggered for employeeId: " + employeeId, employeeId));
        } catch (Exception e) {
            log.error("Failed to re-sync employee exit for employeeId={}: {}", employeeId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Re-sync failed: " + e.getMessage()));
        }
    }
}
