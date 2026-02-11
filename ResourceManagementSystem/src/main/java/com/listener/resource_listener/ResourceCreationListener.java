package com.listener.resource_listener;

import com.entity.resource_entities.Resource;
import com.service_interface.availability_interface.AvailabilityTriggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.YearMonth;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResourceCreationListener {

    private final AvailabilityTriggerService availabilityTriggerService;

    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleResourceCreated(ResourceCreatedEvent event) {
        log.info("Handling resource creation event for resource: {}", event.getResource().getResourceId());
        
        Resource resource = event.getResource();
        
        // Calculate availability for current month and next 11 months
        YearMonth currentMonth = YearMonth.now();
        YearMonth endMonth = currentMonth.plusMonths(11);
        
        YearMonth month = currentMonth;
        while (!month.isAfter(endMonth)) {
            try {
                availabilityTriggerService.triggerResourceRecalculation(resource.getResourceId(), month);
                log.info("Scheduled availability calculation for resource {} month {}", 
                        resource.getResourceId(), month);
            } catch (Exception e) {
                log.error("Failed to schedule availability calculation for resource {} month {}", 
                        resource.getResourceId(), month, e);
            }
            month = month.plusMonths(1);
        }
        
        log.info("Completed availability calculation scheduling for new resource: {}", resource.getResourceId());
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
