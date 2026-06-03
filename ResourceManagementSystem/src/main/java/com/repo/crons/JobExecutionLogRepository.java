package com.repo.crons;

import com.entity.crons.JobExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface JobExecutionLogRepository extends JpaRepository<JobExecutionLog, UUID> {

    List<JobExecutionLog> findByJobNameOrderByStartTimeDesc(String jobName);

    @Modifying
    @Query("DELETE FROM JobExecutionLog j WHERE j.startTime < :cutoff")
    int deleteLogsOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
