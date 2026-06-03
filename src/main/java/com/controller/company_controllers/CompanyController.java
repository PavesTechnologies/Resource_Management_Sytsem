package com.controller.company_controllers;

import com.dto.centralised_dto.ApiResponse;
import com.entity.company_entities.Company;
import com.service_interface.company_service_interface.CompanyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/company")
public class CompanyController {

    @Autowired
    CompanyService companyService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse<?>> createCompany(@Valid @RequestBody Company company) {
        return companyService.createCompany(company);
    }

    @GetMapping("/{companyId}")
    @PreAuthorize("hasAnyRole('Admin','Resource_Manager')")
    public ResponseEntity<ApiResponse<?>> getCompanyById(@PathVariable UUID companyId) {
        return companyService.getCompanyById(companyId);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('Admin','Resource_Manager')")
    public ResponseEntity<ApiResponse<?>> getAllCompanies() {
        return companyService.getAllCompanies();
    }

    @PutMapping("/update/{companyId}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse<?>> updateCompany(@PathVariable UUID companyId, @Valid @RequestBody Company company) {
        return companyService.updateCompany(companyId, company);
    }

    @DeleteMapping("/delete/{companyId}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse<?>> deleteCompany(@PathVariable UUID companyId) {
        return companyService.deleteCompany(companyId);
    }
}
