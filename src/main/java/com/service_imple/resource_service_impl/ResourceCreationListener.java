package com.service_imple.resource_service_impl;

import com.entity.resource_entities.Resource;
import com.service_interface.availability_service_interface.AvailabilityTriggerService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.YearMonth;

@Component
@RequiredArgsConstructor
public class ResourceCreationListener {

    private final AvailabilityTriggerService availabilityTriggerService;

    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleResourceCreated(ResourceCreatedEvent event) {
        
        Resource resource = event.getResource();
        
        // Calculate availability for current month and next 11 months
        YearMonth currentMonth = YearMonth.now();
        YearMonth endMonth = currentMonth.plusMonths(11);
        
        YearMonth month = currentMonth;
        while (!month.isAfter(endMonth)) {
            try {
                availabilityTriggerService.triggerResourceRecalculation(resource.getResourceId(), month);
            } catch (Exception e) {
                // Continue with next month even if current fails
            }
            month = month.plusMonths(1);
        }
    }

    public static class ResourceCreatedEvent {
        private final Resource resource;

        public ResourceCreatedEvent(Resource resource) {
            this.resource = resource;
        }

        public Resource getResource() {
            return resource;
        }
    }
}
