package com.service_interface.company_service_interface;

import com.dto.centralised_dto.ApiResponse;
import com.entity.company_entities.Company;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface CompanyService {
    ResponseEntity<ApiResponse<?>> createCompany(Company company);
    ResponseEntity<ApiResponse<?>> getCompanyById(UUID companyId);
    ResponseEntity<ApiResponse<?>> getAllCompanies();
    ResponseEntity<ApiResponse<?>> updateCompany(UUID companyId, Company company);
    ResponseEntity<ApiResponse<?>> deleteCompany(UUID companyId);
}
