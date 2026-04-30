package com.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.repo.audit_repo.AuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
@RequiredArgsConstructor
public class AuditConfig {

    private final AuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void configureAuditListener() {
        // Inject dependencies into the static entity listener
        AuditEntityListener.setAuditRepository(auditRepository);
        AuditEntityListener.setObjectMapper(objectMapper);
    }
}
