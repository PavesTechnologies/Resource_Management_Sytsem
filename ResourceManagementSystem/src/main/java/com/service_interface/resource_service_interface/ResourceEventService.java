package com.service_interface.resource_service_interface;

import com.entity.resource_entities.Resource;

public interface ResourceEventService {
    
    void publishResourceCreated(Resource resource);
    
    void publishResourceUpdated(Resource resource);
    
    void publishResourceDeleted(Long resourceId);
    
    void triggerLedgerCalculationAfterUpdate(Long resourceId);

    void triggerLedgerCalculationAfterCreate(Long resourceId);

    void triggerLedgerCleanupAfterDelete(Long resourceId);
}
