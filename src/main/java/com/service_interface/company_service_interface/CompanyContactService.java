package com.service_interface.company_service_interface;

import com.dto.centralised_dto.ApiResponse;
import com.entity.company_entities.CompanyEscalationContact;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface CompanyContactService {
    ResponseEntity<ApiResponse<?>> createCompanyContact(CompanyEscalationContact companyContact);
    ResponseEntity<ApiResponse<?>> updateCompanyContact(UUID contactId, CompanyEscalationContact companyContact);
    ResponseEntity<ApiResponse<?>> deleteCompanyContact(UUID id);
    ResponseEntity<ApiResponse<?>> getAllContacts();
}
