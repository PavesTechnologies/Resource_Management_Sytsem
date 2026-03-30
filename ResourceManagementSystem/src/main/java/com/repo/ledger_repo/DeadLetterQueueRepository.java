package com.repo.ledger_repo;

import com.entity.ledger_entities.DeadLetterQueue;
import com.entity_enums.ledger_enums.DLQStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeadLetterQueueRepository extends JpaRepository<DeadLetterQueue, Long> {

    List<DeadLetterQueue> findByStatus(DLQStatus status);

    List<DeadLetterQueue> findByStatusAndNextRetryAtBefore(DLQStatus status, LocalDateTime currentTime);

    List<DeadLetterQueue> findByEventId(String eventId);

    List<DeadLetterQueue> findByResourceId(Long resourceId);

    @Query("SELECT dlq FROM DeadLetterQueue dlq WHERE dlq.status = :status AND dlq.nextRetryAt <= :currentTime ORDER BY dlq.createdAt ASC")
    List<DeadLetterQueue> findReadyForRetry(@Param("status") DLQStatus status, @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT dlq FROM DeadLetterQueue dlq WHERE dlq.status = :status AND dlq.retryCount < dlq.maxRetryCount AND dlq.nextRetryAt <= :currentTime")
    List<DeadLetterQueue> findRetryableEvents(@Param("status") DLQStatus status, @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT COUNT(dlq) FROM DeadLetterQueue dlq WHERE dlq.status = :status AND dlq.originalEventType = :eventType")
    Long countByStatusAndEventType(@Param("status") DLQStatus status, @Param("eventType") String eventType);

    @Query("SELECT dlq FROM DeadLetterQueue dlq WHERE dlq.status = :status AND dlq.retryCount >= dlq.maxRetryCount")
    List<DeadLetterQueue> findExhaustedRetries(@Param("status") DLQStatus status);

    @Query("SELECT dlq FROM DeadLetterQueue dlq WHERE dlq.createdAt < :cutoffDate")
    Page<DeadLetterQueue> findOldEntries(@Param("cutoffDate") LocalDateTime cutoffDate, Pageable pageable);

    @Modifying
    @Query("UPDATE DeadLetterQueue dlq SET dlq.status = :newStatus, dlq.nextRetryAt = :nextRetryAt, dlq.retryCount = dlq.retryCount + 1, dlq.lastRetryAt = :lastRetryAt, dlq.updatedAt = :updatedAt WHERE dlq.id = :id")
    int markForRetry(@Param("id") Long id, @Param("newStatus") DLQStatus newStatus, @Param("nextRetryAt") LocalDateTime nextRetryAt, @Param("lastRetryAt") LocalDateTime lastRetryAt, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Query("UPDATE DeadLetterQueue dlq SET dlq.status = :newStatus, dlq.updatedAt = :updatedAt WHERE dlq.id = :id")
    int markAsManuallyProcessed(@Param("id") Long id, @Param("newStatus") DLQStatus newStatus, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Query("UPDATE DeadLetterQueue dlq SET dlq.status = :exhaustedStatus, dlq.updatedAt = :updatedAt WHERE dlq.id = :id AND dlq.retryCount >= dlq.maxRetryCount")
    int markAsExhausted(@Param("id") Long id, @Param("exhaustedStatus") DLQStatus exhaustedStatus, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Query("DELETE FROM DeadLetterQueue dlq WHERE dlq.status = :status AND dlq.createdAt < :cutoffDate")
    int deleteOldEntries(@Param("status") DLQStatus status, @Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT dlq.originalEventType, COUNT(dlq) FROM DeadLetterQueue dlq WHERE dlq.status = :status GROUP BY dlq.originalEventType")
    List<Object[]> getDLQStatisticsByEventType(@Param("status") DLQStatus status);

    @Query("SELECT dlq.status, COUNT(dlq) FROM DeadLetterQueue dlq GROUP BY dlq.status")
    List<Object[]> getDLQStatisticsByStatus();

    @Query("SELECT dlq FROM DeadLetterQueue dlq WHERE dlq.eventId = :eventId AND dlq.status != :status")
    Optional<DeadLetterQueue> findActiveDLQEntry(@Param("eventId") String eventId, @Param("status") DLQStatus status);

    @Query("SELECT COUNT(dlq) FROM DeadLetterQueue dlq WHERE dlq.resourceId = :resourceId AND dlq.status IN (:statuses)")
    Long countByResourceIdAndStatuses(@Param("resourceId") Long resourceId, @Param("statuses") List<DLQStatus> statuses);
}
