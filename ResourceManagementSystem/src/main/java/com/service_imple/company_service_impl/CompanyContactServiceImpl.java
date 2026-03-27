package com.service_imple.company_service_impl;

import com.dto.centralised_dto.ApiResponse;
import com.entity.company_entities.Company;
import com.entity.company_entities.CompanyEscalationContact;
import com.global_exception_handler.CompanyExceptionHandler;
import com.global_exception_handler.ProjectExceptionHandler;
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

    @Autowired
    private ApiResponse apiResponse;

    @Override
    public ResponseEntity<ApiResponse<?>> createCompanyContact(CompanyEscalationContact companyContact) {
        // Validate required fields
        if (companyContact.getCompany() == null || companyContact.getCompany().getCompanyId() == null) {
            throw new ProjectExceptionHandler(
                HttpStatus.BAD_REQUEST,
                "INVALID_COMPANY",
                "Company is required"
            );
        }
        
        // Check for duplicate email
        if (companyContact.getEmail() != null && 
            companyContactRepo.existsByEmailAndCompany_CompanyId(
                companyContact.getEmail(), 
                companyContact.getCompany().getCompanyId())) {
            throw new ProjectExceptionHandler(
                HttpStatus.CONFLICT,
                "DUPLICATE_EMAIL",
                "Email already exists for this company"
            );
        }
        
        // Check for duplicate contact name
        if (companyContact.getContactName() != null && 
            companyContactRepo.existsByContactNameAndCompany_CompanyId(
                companyContact.getContactName(), 
                companyContact.getCompany().getCompanyId())) {
            throw new ProjectExceptionHandler(
                HttpStatus.CONFLICT,
                "DUPLICATE_CONTACT_NAME",
                "Contact name already exists for this company"
            );
        }
        
        // Handle the company entity - fetch by companyId if provided, create default if not
        Company company = companyContact.getCompany();
        
        // Check if we have a companyId but no full company object
        if (company == null) {
            // Create a default company when none is provided
            company = new Company();
            company.setCompanyName("Default Company");
            company.setCompanyCode("DEF" + System.currentTimeMillis() % 10000); // Generate unique code
            company.setStatus(com.entity_enums.centralised_enums.RecordStatus.ACTIVE);
            company.setPriorityLevel(com.entity_enums.centralised_enums.PriorityLevel.LOW);
            company.setCountryName("United States");
            company.setDefaultTimezone("UTC");
            company.setEscalationContact(true);
            company = companyRepo.save(company);
        } else {
            // If only companyId is provided, fetch the full company
            if (company.getCompanyId() != null && company.getCompanyName() == null) {
                final UUID companyId = company.getCompanyId();
                company = companyRepo.findById(companyId).orElse(null);
                
                if (company == null) {
                    // Company not found, create a new one with the provided ID
                    company = new Company();
                    company.setCompanyId(companyId);
                    company.setCompanyName("Default Company");
                    company.setCompanyCode("DEF" + System.currentTimeMillis() % 10000);
                    company.setStatus(com.entity_enums.centralised_enums.RecordStatus.ACTIVE);
                    company.setPriorityLevel(com.entity_enums.centralised_enums.PriorityLevel.LOW);
                    company.setCountryName("United States");
                    company.setDefaultTimezone("UTC");
                    company.setEscalationContact(true);
                    company = companyRepo.save(company);
                } else {
                    // Check if required fields are missing and populate them
                    if (company.getPriorityLevel() == null) {
                        company.setPriorityLevel(com.entity_enums.centralised_enums.PriorityLevel.LOW);
                    }
                    if (company.getCountryName() == null) {
                        company.setCountryName("United States");
                    }
                    if (company.getDefaultTimezone() == null) {
                        company.setDefaultTimezone("UTC");
                    }
                    if (company.getEscalationContact() == null) {
                        company.setEscalationContact(true);
                    }
                    if (company.getStatus() == null) {
                        company.setStatus(com.entity_enums.centralised_enums.RecordStatus.ACTIVE);
                    }
                    
                    // Only save if we updated missing required fields
                    if (company.getCountryName() != null && company.getDefaultTimezone() != null && 
                        company.getPriorityLevel() != null && company.getEscalationContact() != null) {
                        company = companyRepo.save(company);
                    }
                }
            } else {
                // Set default values for required fields if not provided
                if (company.getStatus() == null) {
                    company.setStatus(com.entity_enums.centralised_enums.RecordStatus.ACTIVE);
                }
                if (company.getPriorityLevel() == null) {
                    company.setPriorityLevel(com.entity_enums.centralised_enums.PriorityLevel.LOW);
                }
                if (company.getCountryName() == null) {
                    company.setCountryName("United States");
                }
                if (company.getDefaultTimezone() == null) {
                    company.setDefaultTimezone("UTC");
                }
                if (company.getEscalationContact() == null) {
                    company.setEscalationContact(true);
                }
                // Ensure company code exists if not provided
                if (company.getCompanyCode() == null || company.getCompanyCode().trim().isEmpty()) {
                    company.setCompanyCode("DEF" + System.currentTimeMillis() % 10000);
                }
                // Save new or updated company
                company = companyRepo.save(company);
            }
        }
        
        companyContact.setCompany(company);
        
        CompanyEscalationContact contact = companyContactRepo.save(companyContact);
        if(contact != null) {
            return ResponseEntity.ok(apiResponse.getAPIResponse(true, "Company Contact Created Successfully", contact));
        }
        else {
            throw new CompanyExceptionHandler("Company Contact creation Failed");
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> updateCompanyContact(UUID contactId, CompanyEscalationContact companyContact) {
        // Validate contact exists
        CompanyEscalationContact existingContact = companyContactRepo.findById(contactId)
                .orElseThrow(() -> new CompanyExceptionHandler("Company Contact not found"));

        // Check for duplicate email (excluding current contact)
        if (companyContact.getEmail() != null && existingContact.getCompany() != null) {
            var existingEmailContact = companyContactRepo.findByEmailAndCompanyIdExcludingId(
                companyContact.getEmail(), 
                existingContact.getCompany().getCompanyId(),
                contactId
            );
            
            if (existingEmailContact.isPresent()) {
                throw new ProjectExceptionHandler(
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
                throw new ProjectExceptionHandler(
                    HttpStatus.CONFLICT,
                    "DUPLICATE_CONTACT_NAME",
                    "Contact name already exists for this company"
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
        
        // Handle company update if provided
        if (companyContact.getCompany() != null) {
            Company company = companyContact.getCompany();
            if (company.getCompanyId() != null) {
                final UUID companyId = company.getCompanyId();
                final Company companyToSave = company;
                company = companyRepo.findById(companyId)
                        .orElseGet(() -> companyRepo.save(companyToSave));
            } else {
                company = companyRepo.save(company);
            }
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
                .orElseThrow(() -> new CompanyExceptionHandler("Company Contact not found"));
        
        companyContactRepo.deleteById(id);
        return ResponseEntity.ok(apiResponse.getAPIResponse(true, "Company Contact Deleted Successfully", contact));
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getAllContacts() {
        List<CompanyEscalationContact> contacts = companyContactRepo.findAll();
        
        if (contacts.isEmpty()) {
            throw new CompanyExceptionHandler("No company contacts found");
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
