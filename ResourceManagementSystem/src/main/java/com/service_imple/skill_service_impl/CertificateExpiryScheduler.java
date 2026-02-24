package com.service_imple.skill_service_impl;


import com.entity.skill_entities.ResourceCertificate;
import com.entity_enums.skill_enums.CertificateStatus;
import com.repo.skill_repo.ResourceCertificateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CertificateExpiryScheduler {
    private final ResourceCertificateRepository resourceCertificateRepository;

    @Scheduled(cron = "0 0 0 * * ?")
//@Scheduled(fixedRate = 10000)
@Transactional
    public void updateStatuses() {
//    System.out.println("Updating certificate statuses...");
//    System.out.println("Today is: " + LocalDate.now());
        List<ResourceCertificate> list = resourceCertificateRepository.findByActiveFlagTrue();
    for (ResourceCertificate rc : list) {

//        System.out.println("Checking expiry date: " + rc.getExpiryDate());

        if (rc.getExpiryDate() == null) continue;

        CertificateStatus newStatus =
                calculateStatus(rc.getExpiryDate());

//        System.out.println("Calculated Status: " + newStatus);
//        System.out.println("Old Status: " + rc.getStatus());

        rc.setStatus(newStatus);
    }
        resourceCertificateRepository.saveAll(list);
    }
    private CertificateStatus calculateStatus(LocalDate expiryDate) {
        LocalDate today = LocalDate.now();

        if (expiryDate.isBefore(today)) {
            return CertificateStatus.EXPIRED;
        }

        if (expiryDate.isBefore(today.plusDays(30))) {
            return CertificateStatus.EXPIRING_SOON;
        }

        return CertificateStatus.ACTIVE;
    }
}
