package com.controller.company_controllers;

import com.dto.ApiResponse;
import com.entity.company_entities.CompanyEscalationContact;
import com.service_interface.company_service_interface.CompanyContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/company-contact")
public class CompanyContactController {
    @Autowired
    CompanyContactService companyContactService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> createCompanyContact(@RequestBody CompanyEscalationContact companyContact) {
        return companyContactService.createCompanyContact(companyContact);
    }

    @PutMapping("/update/{contactId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> updateCompanyContact(@PathVariable UUID contactId, @RequestBody CompanyEscalationContact companyContact) {
        return companyContactService.updateCompanyContact(contactId, companyContact);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> deleteCompanyContact(@PathVariable UUID id) {
        return companyContactService.deleteCompanyContact(id);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<?>> getAllContacts() {
        return companyContactService.getAllContacts();
    }

}
