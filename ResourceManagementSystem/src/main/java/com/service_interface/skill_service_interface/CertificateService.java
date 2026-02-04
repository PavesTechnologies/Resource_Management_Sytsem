package com.service_interface.skill_service_interface;

import com.entity.skill_entities.Certificate;
import com.dto.UserDTO;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface CertificateService {

    Certificate assignCertificate(
            UUID skillId,
            String certificateId,
            String providerName,
            String certificateFile,
            LocalDate certifiedAt,
            LocalDate expiryDate,
            UserDTO user
    );

    Optional<Certificate> getActiveCertificate(UUID skillId);
}
