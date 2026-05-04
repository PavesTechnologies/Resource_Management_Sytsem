package com.events.publisher;

import com.events.ledger_events.AllocationChangedEvent;
import com.events.ledger_events.BaseLedgerEvent;
import com.events.ledger_events.ResourceCreatedEvent;
import com.events.ledger_events.RoleOffLedgerEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublishingService {

    private final LedgerEventPublisher ledgerEventPublisher;

    @Async("ledgerEventExecutor")
    @Retryable(retryFor = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public CompletableFuture<Void> publishAllocationChangedAsync(AllocationChangedEvent event) {
        try {
            ledgerEventPublisher.publishAllocationChangedEvent(event);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Async publishing failed for allocation changed event: {}", e.getMessage(), e);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Async("ledgerEventExecutor")
    @Retryable(retryFor = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public CompletableFuture<Void> publishRoleOffAsync(RoleOffLedgerEvent event) {
        try {
            ledgerEventPublisher.publishRoleOffEvent(event);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Async publishing failed for role-off event: {}", e.getMessage(), e);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Async("ledgerEventExecutor")
    @Retryable(retryFor = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public CompletableFuture<Void> publishResourceCreatedAsync(ResourceCreatedEvent event) {
        try {
            ledgerEventPublisher.publishResourceCreatedEvent(event);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Async publishing failed for resource created event: {}", e.getMessage(), e);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Async("ledgerEventExecutor")
    @Retryable(retryFor = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public CompletableFuture<Void> publishGenericAsync(BaseLedgerEvent event) {
        try {
            ledgerEventPublisher.publishGenericLedgerEvent(event);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Async publishing failed for generic ledger event: {}", e.getMessage(), e);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    public void publishAllocationChangedSync(AllocationChangedEvent event) {
        ledgerEventPublisher.publishAllocationChangedEvent(event);
    }

    public void publishRoleOffSync(RoleOffLedgerEvent event) {
        ledgerEventPublisher.publishRoleOffEvent(event);
    }

    public void publishResourceCreatedSync(ResourceCreatedEvent event) {
        ledgerEventPublisher.publishResourceCreatedEvent(event);
    }

    public void publishGenericSync(BaseLedgerEvent event) {
        ledgerEventPublisher.publishGenericLedgerEvent(event);
    }
}
