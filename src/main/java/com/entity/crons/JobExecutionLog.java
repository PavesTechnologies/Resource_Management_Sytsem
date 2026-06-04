package com.entity.crons;

import com.entity_enums.crons_emuns.JobStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_execution_log")
@Getter
@Setter
public class JobExecutionLog {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    private String jobName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private JobStatus status;   // RUNNING, SUCCESS, FAILED

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private String nodeIdentifier;
    private Integer attempt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Version
    private Integer version;

    // getters + setters
}
