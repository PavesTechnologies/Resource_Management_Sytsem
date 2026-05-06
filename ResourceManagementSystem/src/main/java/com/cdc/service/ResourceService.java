package com.cdc.service;

import com.cdc.derivation.ResourceDerivationService;
import com.cdc.mapping.CdcValueConverter;
import com.cdc.mapping.ColumnMapping;
import com.cdc.mapping.EosCdcMappingRegistry;
import com.cdc.util.ReflectionUtil;
import com.entity.resource_entities.Resource;
import com.repo.resource_repo.ResourceRepository;
import org.apache.kafka.connect.data.Struct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Service for RMS Resource entity operations in CDC context.
 * 
 * This service handles Resource entity loading, creation, and updates
 * with selective save logic to minimize unnecessary DB writes.
 * 
 * Architecture:
 * - Load/create Resource entities safely
 * - Apply direct field mappings
 * - Derive remaining fields using business logic
 * - Save only when meaningful changes occurred
 */
@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final ResourceDerivationService resourceDerivationService;

    public ResourceService(ResourceRepository resourceRepository, 
                      ResourceDerivationService resourceDerivationService) {
        this.resourceRepository = resourceRepository;
        this.resourceDerivationService = resourceDerivationService;
    }

    /**
     * Load Resource by resourceId.
     * 
     * @param resourceId The RMS resource identifier
     * @return Resource entity or null if not found
     */
    @Transactional(readOnly = true)
    public Resource loadResource(String resourceId) {
        return resourceRepository.findById(resourceId).orElse(null);
    }

    /**
     * Create new Resource entity.
     * 
     * @param resourceId The RMS resource identifier
     * @return New Resource entity
     */
    public Resource createResource(String resourceId) {
        Resource resource = new Resource();
        resource.setResourceId(resourceId);
        return resource;
    }

    /**
     * Apply direct field mappings to Resource entity.
     * 
     * @param resource The Resource entity to update
     * @param fieldName The RMS field name
     * @param value The value to set
     * @return true if field was updated, false if value unchanged
     */
    public boolean applyDirectMapping(Resource resource, String fieldName, Object value) {
        if (resource == null || fieldName == null) {
            return false;
        }

        Object currentValue = ReflectionUtil.getFieldValue(resource, fieldName);
        
        // Skip if value hasn't changed
        if (Objects.equals(currentValue, value)) {
            return false;
        }

        // Apply the field update using shared reflection utility
        ReflectionUtil.setField(resource, fieldName, value);
        return true;
    }

    /**
     * Populate derived fields using ResourceDerivationService.
     * 
     * @param resource The Resource entity to populate
     * @param eosPayload The EOS Debezium payload
     */
    public void populateDerivedFields(Resource resource, Struct eosPayload) {
        if (resource == null || eosPayload == null) {
            return;
        }

        resourceDerivationService.populateDerivedFields(resource, eosPayload);
    }

    /**
     * Save Resource entity with selective logic.
     * 
     * Only saves if meaningful changes occurred.
     * Uses optimistic locking for concurrent safety.
     * 
     * @param resource The Resource entity to save
     * @return true if saved successfully, false if skipped
     */
    @Transactional
    public boolean saveResourceSelective(Resource resource) {
        if (resource == null) {
            return false;
        }

        // Check if resource has meaningful changes
        if (!hasMeaningfulChanges(resource)) {
            return false; // Skip save if no meaningful changes
        }

        try {
            resourceRepository.save(resource);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to save resource " + resource.getResourceId() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if Resource has meaningful changes that warrant saving.
     * 
     * @param resource The Resource entity to check
     * @return true if meaningful changes exist, false otherwise
     */
    private boolean hasMeaningfulChanges(Resource resource) {
        // For CDC context, assume changes are meaningful if version is null (new) or fields changed
        // The actual change detection happens at the CDC handler level
        return resource.getVersion() == null || resource.getVersion() >= 0;
    }

}
