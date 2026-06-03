package com.service_imple.company_service_impl;

import com.dto.centralised_dto.ApiResponse;
import com.entity.company_entities.Company;
import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.centralised_enums.RecordStatus;
import com.global_exception_handler.ClientExceptionHandler;
import com.repo.company_repo.CompanyRepo;
import com.service_interface.company_service_interface.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CompanyServiceImpl implements CompanyService {

    @Autowired
    private CompanyRepo companyRepo;

    @Autowired
    private ApiResponse apiResponse;

    @Override
    public ResponseEntity<ApiResponse<?>> createCompany(Company company) {
        // Validate company code uniqueness
        if (companyRepo.existsByCompanyCode(company.getCompanyCode())) {
            throw ClientExceptionHandler.badRequest("Company code already exists");
        }

        // Set default values if not provided
        if (company.getStatus() == null) {
            company.setStatus(RecordStatus.ACTIVE);
        }
        if (company.getEscalationContact() == null) {
            company.setEscalationContact(true);
        }

        Company savedCompany = companyRepo.save(company);
        if (savedCompany != null) {
            return ResponseEntity.ok(apiResponse.getAPIResponse(true, "Company Created Successfully", savedCompany));
        } else {
            throw ClientExceptionHandler.badRequest("Company creation Failed");
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getCompanyById(UUID companyId) {
        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> ClientExceptionHandler.badRequest("Company not found"));
        
        return ResponseEntity.ok(apiResponse.getAPIResponse(true, "Company Found Successfully", company));
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getAllCompanies() {
        List<Company> companies = companyRepo.findAll();
        
        if (companies.isEmpty()) {
            throw ClientExceptionHandler.badRequest("No companies found");
        }

        return ResponseEntity.ok(apiResponse.getAPIResponse(true, "All Companies Fetched Successfully", companies));
    }

    @Override
    public ResponseEntity<ApiResponse<?>> updateCompany(UUID companyId, Company company) {
        Company existingCompany = companyRepo.findById(companyId)
                .orElseThrow(() -> ClientExceptionHandler.badRequest("Company not found"));

        // Check if company code is being changed and if it's already taken
        if (!existingCompany.getCompanyCode().equals(company.getCompanyCode()) &&
            companyRepo.existsByCompanyCode(company.getCompanyCode())) {
            throw ClientExceptionHandler.badRequest("Company code already exists");
        }

        // Update fields
        existingCompany.setCompanyName(company.getCompanyName());
        existingCompany.setCompanyCode(company.getCompanyCode());
        existingCompany.setPriorityLevel(company.getPriorityLevel());
        existingCompany.setCountryName(company.getCountryName());
        existingCompany.setDefaultTimezone(company.getDefaultTimezone());
        existingCompany.setStatus(company.getStatus());
        existingCompany.setEscalationContact(company.getEscalationContact());

        Company updatedCompany = companyRepo.save(existingCompany);
        return ResponseEntity.ok(apiResponse.getAPIResponse(true, "Company Updated Successfully", updatedCompany));
    }

    @Override
    public ResponseEntity<ApiResponse<?>> deleteCompany(UUID companyId) {
        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> ClientExceptionHandler.badRequest("Company not found"));
        
        companyRepo.deleteById(companyId);
        return ResponseEntity.ok(apiResponse.getAPIResponse(true, "Company Deleted Successfully", company));
    }
}
