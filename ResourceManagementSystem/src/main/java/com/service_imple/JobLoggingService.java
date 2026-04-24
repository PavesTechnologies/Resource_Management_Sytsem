package com.service_imple;

import com.entity.crons.JobExecutionLog;
import com.entity_enums.crons_emuns.JobStatus;
import com.repo.crons.JobExecutionLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class JobLoggingService {

    private final JobExecutionLogRepository repo;

    public JobLoggingService(JobExecutionLogRepository repo) {
        this.repo = repo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID createJobLog(String jobName, String nodeId) {
        JobExecutionLog log = new JobExecutionLog();
        log.setJobName(jobName);
        log.setStatus(JobStatus.RUNNING);
        log.setStartTime(LocalDateTime.now());
        log.setNodeIdentifier(nodeId);
        log.setAttempt(1);
        repo.save(log);
        return log.getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateJobLog(UUID logId, boolean success, String errorMessage) {
        repo.findById(logId).ifPresent(entry -> {
            entry.setEndTime(LocalDateTime.now());
            entry.setDurationMs(
                    Duration.between(entry.getStartTime(), entry.getEndTime()).toMillis()
            );
            entry.setStatus(success ? JobStatus.SUCCESS : JobStatus.FAILED);
            entry.setErrorMessage(errorMessage);
            repo.save(entry);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteOldLogs(int daysToRetain) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysToRetain);
        int deleted = repo.deleteLogsOlderThan(cutoff);
        log.info("Deleted {} job execution logs older than {} days", deleted, daysToRetain);
    }
}
