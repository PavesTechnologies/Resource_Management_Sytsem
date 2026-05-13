package com.service_imple.company_service_impl;

import com.dto.centralised_dto.ApiResponse;
import com.entity.company_entities.Company;
import com.entity.company_entities.CompanyEscalationContact;
import com.global_exception_handler.ClientExceptionHandler;
import com.repo.company_repo.CompanyContactRepo;
import com.repo.company_repo.CompanyRepo;
import com.service_interface.company_service_interface.CompanyContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CompanyContactServiceImpl implements CompanyContactService {
    @Autowired
    private CompanyContactRepo companyContactRepo;

    @Autowired
    private CompanyRepo companyRepo;

    private final ApiResponse apiResponse = new ApiResponse();

    /**
     * Find or create default company
     */
    private Company findOrCreateDefaultCompany() {
        // First try to find existing default companies
        List<Company> existingDefaults = companyRepo.findByCompanyName("Default Company");
        
        if (!existingDefaults.isEmpty()) {
            // Use the first existing default company
            Company firstDefault = existingDefaults.get(0);
            System.out.println("DEBUG: Found existing default company (using first of " + existingDefaults.size() + "): " + firstDefault.getCompanyId());
            return firstDefault;
        }
        
        // Create new default company if not found
        Company defaultCompany = Company.builder()
                .companyName("Default Company")
                .companyCode("DEFAULT001")
                .priorityLevel(com.entity_enums.centralised_enums.PriorityLevel.MEDIUM)
                .countryName("Default")
                .defaultTimezone("UTC")
                .status(com.entity_enums.centralised_enums.RecordStatus.ACTIVE)
                .escalationContact(true)
                .build();
        
        Company savedDefault = companyRepo.save(defaultCompany);
        System.out.println("DEBUG: Created new default company: " + savedDefault.getCompanyId());
        return savedDefault;
    }

    @Override
    public ResponseEntity<ApiResponse<?>> createCompanyContact(CompanyEscalationContact companyContact) {
        // Debug logging
        System.out.println("DEBUG: companyContact.getCompany() = " + companyContact.getCompany());
        if (companyContact.getCompany() != null) {
            System.out.println("DEBUG: companyContact.getCompany().getCompanyId() = " + companyContact.getCompany().getCompanyId());
        }
        
        // Validate required fields
        UUID companyId = null;
        Company company = null;
        
        // Try to get companyId from existing company object
        if (companyContact.getCompany() != null && companyContact.getCompany().getCompanyId() != null) {
            companyId = companyContact.getCompany().getCompanyId();
            System.out.println("DEBUG: Extracted companyId from company object: " + companyId);
        }
        
        // If companyId exists, try to find the company
        if (companyId != null) {
            company = companyRepo.findById(companyId).orElse(null);
        }
        
        // If company doesn't exist, try to auto-create it or use default
        if (company == null) {
            System.out.println("DEBUG: Company not found, checking for auto-creation or default...");
            
            // If company name and code are provided, create specific company
            if ((companyContact.getCompanyName() != null && !companyContact.getCompanyName().trim().isEmpty()) &&
                (companyContact.getCompanyCode() != null && !companyContact.getCompanyCode().trim().isEmpty())) {
                
                // Check if company code already exists
                if (companyRepo.existsByCompanyCode(companyContact.getCompanyCode())) {
                    throw new ClientExceptionHandler(HttpStatus.CONFLICT, "DUPLICATE_COMPANY_CODE", "Company code already exists");
                }
                
                // Create new company with provided values
                Company newCompany = Company.builder()
                        .companyName(companyContact.getCompanyName())
                        .companyCode(companyContact.getCompanyCode())
                        .priorityLevel(com.entity_enums.centralised_enums.PriorityLevel.MEDIUM)
                        .countryName("Default")
                        .defaultTimezone("UTC")
                        .status(com.entity_enums.centralised_enums.RecordStatus.ACTIVE)
                        .escalationContact(true)
                        .build();
                
                company = companyRepo.save(newCompany);
                System.out.println("DEBUG: Auto-created specific company with ID: " + company.getCompanyId());
            } else {
                // Use default company
                company = findOrCreateDefaultCompany();
                System.out.println("DEBUG: Using default company: " + company.getCompanyId());
            }
        }
        
        companyId = company.getCompanyId();
        System.out.println("DEBUG: Using company ID: " + companyId);

        // Step 2: All duplicate checks AFTER confirming company is real
        if (companyContact.getEmail() != null &&
                companyContactRepo.existsByEmailAndCompany_CompanyId(companyContact.getEmail(), companyId)) {
            throw new ClientExceptionHandler(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "Email already exists for this company");
        }

        if (companyContact.getContactName() != null &&
                companyContactRepo.existsByContactNameAndCompany_CompanyId(companyContact.getContactName(), companyId)) {
            throw new ClientExceptionHandler(HttpStatus.CONFLICT, "DUPLICATE_CONTACT_NAME", "Contact name already exists for this company");
        }

        // Step 3: Enhanced role+level duplicate check - THIS IS THE KEY PART
        if (companyContact.getContactRole() != null && companyContact.getEscalationLevel() != null) {
            System.out.println("DEBUG: Checking for duplicate - companyId: " + companyId);
            System.out.println("DEBUG: Checking for duplicate - contactRole: " + companyContact.getContactRole());
            System.out.println("DEBUG: Checking for duplicate - escalationLevel: " + companyContact.getEscalationLevel());
            
            Long count = companyContactRepo.countByCompanyIdAndContactRoleAndEscalationLevel(
                    companyId,
                    companyContact.getContactRole(),
                    companyContact.getEscalationLevel()
            );
            
            System.out.println("DEBUG: Duplicate count found: " + count);
            
            if (count > 0) {
                System.out.println("DEBUG: DUPLICATE FOUND! Throwing exception...");
                throw new ClientExceptionHandler(
                        HttpStatus.CONFLICT, "DUPLICATE_ROLE_LEVEL",
                        "Contact role '" + companyContact.getContactRole() + "' and escalation level '" + companyContact.getEscalationLevel() + "' already exists for this company");
            } else {
                System.out.println("DEBUG: No duplicate found, proceeding with creation...");
            }
        } else {
            System.out.println("DEBUG: Skipping duplicate check - contactRole or escalationLevel is null");
        }

        // Step 4: Set real company object
        companyContact.setCompany(company);

        CompanyEscalationContact contact = companyContactRepo.save(companyContact);
        if (contact != null) {
            return ResponseEntity.ok(apiResponse.getAPIResponse(true, "Company Contact Created Successfully", contact));
        } else {
            throw ClientExceptionHandler.badRequest("Company Contact creation Failed");
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> updateCompanyContact(UUID contactId, CompanyEscalationContact companyContact) {
        // Validate contact exists
        CompanyEscalationContact existingContact = companyContactRepo.findById(contactId)
                .orElseThrow(() -> ClientExceptionHandler.badRequest("Company Contact not found"));

        // Check for duplicate email (excluding current contact)
        if (companyContact.getEmail() != null && existingContact.getCompany() != null) {
            var existingEmailContact = companyContactRepo.findByEmailAndCompanyIdExcludingId(
                companyContact.getEmail(), 
                existingContact.getCompany().getCompanyId(),
                contactId
            );
            
            if (existingEmailContact.isPresent()) {
                throw new ClientExceptionHandler(
                    HttpStatus.CONFLICT,
                    "DUPLICATE_EMAIL",
                    "Email already exists for this company"
                );
            }
        }
        
        // Check for duplicate contact name (excluding current contact)
        if (companyContact.getContactName() != null && existingContact.getCompany() != null) {
            var existingNameContact = companyContactRepo.findByContactNameAndCompanyIdExcludingId(
                companyContact.getContactName(), 
                existingContact.getCompany().getCompanyId(),
                contactId
            );
            
            if (existingNameContact.isPresent()) {
                throw new ClientExceptionHandler(
                    HttpStatus.CONFLICT,
                    "DUPLICATE_CONTACT_NAME",
                    "Contact name already exists for this company"
                );
            }
        }

        // Check for duplicate contact role and escalation level combination (excluding current contact)
        if (companyContact.getContactRole() != null && companyContact.getEscalationLevel() != null && existingContact.getCompany() != null) {
            var existingRoleLevelContact = companyContactRepo.findByCompanyIdAndContactRoleAndEscalationLevelExcludingId(
                existingContact.getCompany().getCompanyId(),
                companyContact.getContactRole(),
                companyContact.getEscalationLevel(),
                contactId
            );
            
            if (existingRoleLevelContact.isPresent()) {
                throw new ClientExceptionHandler(
                    HttpStatus.CONFLICT,
                    "DUPLICATE_ROLE_LEVEL",
                    "Contact role and escalation level combination already exists for this company"
                );
            }
        }

        // Update all fields from the request
        if (companyContact.getContactName() != null) {
            existingContact.setContactName(companyContact.getContactName());
        }
        if (companyContact.getContactRole() != null) {
            existingContact.setContactRole(companyContact.getContactRole());
        }
        if (companyContact.getEmail() != null) {
            existingContact.setEmail(companyContact.getEmail());
        }
        if (companyContact.getPhone() != null) {
            existingContact.setPhone(companyContact.getPhone());
        }
        if (companyContact.getActiveFlag() != null) {
            existingContact.setActiveFlag(companyContact.getActiveFlag());
        }
        if (companyContact.getEscalationLevel() != null) {
            existingContact.setEscalationLevel(companyContact.getEscalationLevel());
        }

        if (companyContact.getCompany() != null) {
            UUID companyId = companyContact.getCompany().getCompanyId();

            Company company = companyRepo.findById(companyId)
                    .orElseThrow(() -> new ClientExceptionHandler(
                            HttpStatus.NOT_FOUND,
                            "COMPANY_NOT_FOUND",
                            "Company not found"
                    ));

            existingContact.setCompany(company);
        }

        CompanyEscalationContact updatedContact = companyContactRepo.save(existingContact);
        return ResponseEntity.ok(
                apiResponse.getAPIResponse(
                        true,
                        "Company Contact Updated Successfully",
                        updatedContact
                )
        );
    }

    @Override
    public ResponseEntity<ApiResponse<?>> deleteCompanyContact(UUID id) {
        CompanyEscalationContact contact = companyContactRepo.findById(id)
                .orElseThrow(() -> ClientExceptionHandler.badRequest("Company Contact not found"));
        
        companyContactRepo.deleteById(id);
        return ResponseEntity.ok(apiResponse.getAPIResponse(true, "Company Contact Deleted Successfully", contact));
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getAllContacts() {
        List<CompanyEscalationContact> contacts = companyContactRepo.findAll();
        
        if (contacts.isEmpty()) {
            throw ClientExceptionHandler.badRequest("No company contacts found");
        }

        return ResponseEntity.ok(
                apiResponse.getAPIResponse(
                        true,
                        "All Company Contacts Fetched Successfully",
                        contacts
                )
        );
    }

}
