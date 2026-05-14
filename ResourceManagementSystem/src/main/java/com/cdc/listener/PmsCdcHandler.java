package com.cdc.listener;

import com.cdc.mapping.CdcValueConverter;
import com.cdc.mapping.ColumnMapping;
import com.cdc.mapping.PmsCdcMappingRegistry;
import com.cdc.payload.CdcEventPayload;
import com.cdc.protection.StaleEventProtectionService;
import com.cdc.service.CdcInboxService;
import com.cdc.throttling.ReplayThrottlingService;
import com.cdc.util.CdcUtcSupport;
import com.cdc.util.ReflectionUtil;
import com.entity.project_entities.Project;
import com.entity_enums.project_enums.ProjectDataStatus;
import com.entity_enums.project_enums.ProjectStatus;
import com.repo.client_repo.ClientRepo;
import com.repo.project_repo.ProjectRepository;
import com.service_imple.availability_service_impl.ProjectTimelineChangeService;
import com.service_imple.project_service_impl.ProjectReadinessUpdaterService;
import io.debezium.engine.RecordChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class PmsCdcHandler {

    private final ProjectRepository projectRepository;
    private final ProjectReadinessUpdaterService readinessUpdater;
    private final ClientRepo clientRepo;
    private final ProjectTimelineChangeService projectTimelineChangeService;
    private final StaleEventProtectionService staleEventProtectionService;
    private final ReplayThrottlingService replayThrottlingService;
    private final CdcValueConverter cdcValueConverter;
    private final CdcInboxService cdcInboxService;
    private final CdcUtcSupport cdcUtcSupport;

    public PmsCdcHandler(ProjectRepository projectRepository,
                         ProjectReadinessUpdaterService readinessUpdater,
                         ClientRepo clientRepo,
                         ProjectTimelineChangeService projectTimelineChangeService,
                         StaleEventProtectionService staleEventProtectionService,
                         ReplayThrottlingService replayThrottlingService,
                         CdcValueConverter cdcValueConverter,
                         CdcInboxService cdcInboxService,
                         CdcUtcSupport cdcUtcSupport) {
        this.projectRepository = projectRepository;
        this.readinessUpdater = readinessUpdater;
        this.clientRepo = clientRepo;
        this.projectTimelineChangeService = projectTimelineChangeService;
        this.staleEventProtectionService = staleEventProtectionService;
        this.replayThrottlingService = replayThrottlingService;
        this.cdcValueConverter = cdcValueConverter;
        this.cdcInboxService = cdcInboxService;
        this.cdcUtcSupport = cdcUtcSupport;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleEvent(RecordChangeEvent<SourceRecord> event) {
        Struct value = (Struct) event.record().value();
        if (value == null) {
            return;
        }

        String rawOp = value.getString("op");
        String op = "r".equals(rawOp) ? "c" : rawOp;
        Struct before = value.getStruct("before");
        Struct after = value.getStruct("after");
        String tableName = extractTableName(event);
        String entityId = extractEntityId(after != null ? after : before);
        String entityType = "PMS-" + tableName;

        ReplayThrottlingService.ThrottlingResult throttlingResult = replayThrottlingService.checkReplayAllowed(entityType);
        if (!throttlingResult.isAllowed()) {
            log.warn("Replay throttled for entityId={}, entityType={}, reason={}", entityId, entityType, throttlingResult.getReason());
            return;
        }

        cdcInboxService.persist("PMS", tableName, op, entityType, entityId, event);
    }

    @Transactional
    public void processInboxEvent(CdcEventPayload payload) {
        long startTime = System.currentTimeMillis();
        try {
            processPmsEvent(payload);
        } finally {
            replayThrottlingService.recordReplayCompletion(
                    payload.getEntityType(),
                    System.currentTimeMillis() - startTime
            );
        }
    }

    private void processPmsEvent(CdcEventPayload payload) {
        if ("d".equals(payload.getOperation())) {
            handlePmsDelete(payload);
            return;
        }

        if (!"projects".equals(payload.getTableName()) || payload.getAfter() == null) {
            return;
        }

        Long pmsProjectId = extractPmsProjectId(payload.getAfter());
        if (pmsProjectId == null) {
            throw new IllegalStateException("Failed to extract PMS project ID from CDC payload");
        }

        Set<String> changedColumns = detectChangedColumns(payload);
        if (changedColumns.isEmpty()) {
            log.debug("Skipping PMS update - no columns changed for projectId={}", pmsProjectId);
            return;
        }

        Project project = loadOrCreateProject(pmsProjectId, payload.getAfter());
        if (project == null) {
            throw new IllegalStateException("Failed to load or create project " + pmsProjectId);
        }

        if (staleEventProtectionService.isStaleEvent(project.getChangedAt(), payload.getSourceTimestamp(), String.valueOf(pmsProjectId))) {
            log.warn("Rejecting stale PMS event for projectId={}", pmsProjectId);
            return;
        }

        boolean hasDirectChanges = applyDirectFieldMappings(project, changedColumns, payload.getAfter());
        populateDerivedFields(project, payload.getAfter(), changedColumns, payload.getSourceTimestamp());

        if (saveProjectSelective(project)) {
            log.info("PMS CDC event processed - projectId={}, directChanges={}", pmsProjectId, hasDirectChanges);
        }
    }

    private Project loadOrCreateProject(Long pmsProjectId, Map<String, Object> after) {
        try {
            String projectName = getString(after, "name");
            if (projectName == null) {
                projectName = "New Project";
            }

            UUID clientId = null;
            Object clientIdValue = after.get("client_id");
            if (clientIdValue != null) {
                try {
                    clientId = UUID.fromString(clientIdValue.toString());
                    if (!clientRepo.existsById(clientId)) {
                        clientId = null;
                    }
                } catch (Exception ignored) {
                    clientId = null;
                }
            }

            LocalDateTime now = cdcUtcSupport.utcDateTime(cdcUtcSupport.now());
            projectRepository.upsertSkeleton(pmsProjectId, projectName, now, clientId);
            return projectRepository.findByIdWithLock(pmsProjectId).orElse(null);
        } catch (Exception ex) {
            log.error("Failed to load/create project for PMS ID {}: {}", pmsProjectId, ex.getMessage(), ex);
            return null;
        }
    }

    private boolean applyDirectFieldMappings(Project project, Set<String> changedColumns, Map<String, Object> after) {
        boolean hasChanges = false;
        for (String pmsColumn : changedColumns) {
            ColumnMapping mapping = PmsCdcMappingRegistry.PMS_TO_RMS.get(pmsColumn);
            if (mapping == null) {
                continue;
            }

            Object rawValue = after.get(pmsColumn);
            Object converted = cdcValueConverter.convert(rawValue, mapping.getFieldType(), mapping.getEnumClass());

            if ("client_id".equals(pmsColumn) && converted instanceof UUID uuid && !clientRepo.existsById(uuid)) {
                log.debug("Skipping client_id mapping because client is not yet present in RMS: {}", converted);
                continue;
            }

            try {
                Object currentValue = ReflectionUtil.getFieldValue(project, mapping.getRmsField());
                if (Objects.equals(currentValue, converted)) {
                    continue;
                }
                ReflectionUtil.setField(project, mapping.getRmsField(), converted);
                hasChanges = true;
            } catch (Exception ex) {
                log.error("Failed to apply direct mapping {} for PMS project {}: {}",
                        mapping.getRmsField(), project.getPmsProjectId(), ex.getMessage(), ex);
            }
        }
        return hasChanges;
    }

    private void populateDerivedFields(Project project,
                                       Map<String, Object> after,
                                       Set<String> changedColumns,
                                       Instant sourceTimestamp) {
        boolean isNewProject = project.getCreatedAt() == null;

        if (changedColumns.contains("risk_level")
                && !changedColumns.contains("risk_level_updated_at")
                && !changedColumns.contains("riskLevelUpdatedAt")) {
            project.setRiskLevelUpdatedAt(cdcUtcSupport.utcDateTime(cdcUtcSupport.now()));
        }

        if (isNewProject) {
            LocalDateTime srcCreatedAt = getLocalDateTime(after, "created_at");
            project.setCreatedAt(srcCreatedAt != null ? srcCreatedAt : cdcUtcSupport.utcDateTime(cdcUtcSupport.now()));
            project.setDataStatus(calculateDataStatus(project));
        } else if (hasRelevantFieldChanges(changedColumns)) {
            project.setDataStatus(calculateDataStatus(project));
        }

        LocalDateTime srcChangedAt = getLocalDateTime(after, "updated_at");
        project.setChangedAt(srcChangedAt != null ? srcChangedAt : cdcUtcSupport.utcDateTime(sourceTimestamp != null ? sourceTimestamp : cdcUtcSupport.now()));
        project.setLastSyncedAt(cdcUtcSupport.utcDateTime(cdcUtcSupport.now()));
    }

    private boolean saveProjectSelective(Project project) {
        if (project == null) {
            return false;
        }

        try {
            projectRepository.saveAndFlush(project);
            try {
                readinessUpdater.updateReadiness(project);
            } catch (Exception ex) {
                log.error("Failed to update readiness for project {}: {}", project.getPmsProjectId(), ex.getMessage(), ex);
            }

            try {
                projectTimelineChangeService.handleProjectTimelineChange(
                        project.getPmsProjectId(), null, null, project.getStartDate(), project.getEndDate());
            } catch (Exception ex) {
                log.error("Failed to handle timeline change for project {}: {}", project.getPmsProjectId(), ex.getMessage(), ex);
            }
            return true;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to save project " + project.getPmsProjectId(), ex);
        }
    }

    private void handlePmsDelete(CdcEventPayload payload) {
        Long pmsProjectId = extractPmsProjectId(payload.getBefore());
        if (pmsProjectId == null) {
            return;
        }

        Project project = projectRepository.findByIdWithLock(pmsProjectId).orElse(null);
        if (project == null) {
            return;
        }
        if (staleEventProtectionService.isStaleEvent(project.getChangedAt(), payload.getSourceTimestamp(), String.valueOf(pmsProjectId))) {
            log.warn("Rejecting stale PMS delete event for projectId={}", pmsProjectId);
            return;
        }

        project.setProjectStatus(ProjectStatus.DELETED);
        project.setLastSyncedAt(cdcUtcSupport.utcDateTime(cdcUtcSupport.now()));
        project.setChangedAt(cdcUtcSupport.utcDateTime(payload.getSourceTimestamp() != null ? payload.getSourceTimestamp() : cdcUtcSupport.now()));
        projectRepository.save(project);
        log.info("PMS CDC delete processed successfully for projectId={}", pmsProjectId);
    }

    private Set<String> detectChangedColumns(CdcEventPayload payload) {
        if (payload.getBefore() == null || payload.getAfter() == null) {
            return payload.getAfter() != null ? new LinkedHashSet<>(payload.getAfter().keySet()) : Set.of();
        }
        Set<String> changedColumns = new LinkedHashSet<>();
        payload.getAfter().forEach((key, value) -> {
            if (!Objects.equals(value, payload.getBefore().get(key))) {
                changedColumns.add(key);
            }
        });
        return changedColumns;
    }

    private String extractTableName(RecordChangeEvent<SourceRecord> event) {
        Struct value = (Struct) event.record().value();
        Struct source = value != null ? value.getStruct("source") : null;
        return source != null ? source.getString("table") : "unknown";
    }

    private String extractEntityId(Struct struct) {
        if (struct == null) {
            return "unknown";
        }
        Object id = struct.get("id");
        return id != null ? id.toString() : "unknown";
    }

    private Long extractPmsProjectId(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        Object idValue = data.get("id");
        if (idValue instanceof Number number) {
            return number.longValue();
        }
        try {
            return idValue != null ? Long.parseLong(idValue.toString()) : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private String getString(Map<String, Object> data, String field) {
        if (data == null) {
            return null;
        }
        Object value = data.get(field);
        return value != null ? value.toString() : null;
    }

    private LocalDateTime getLocalDateTime(Map<String, Object> data, String field) {
        if (data == null) {
            return null;
        }
        Instant instant = cdcUtcSupport.extractInstant(data.get(field));
        return instant != null ? cdcUtcSupport.utcDateTime(instant) : null;
    }

    private ProjectDataStatus calculateDataStatus(Project project) {
        if (project.getName() == null || project.getName().trim().isEmpty()) {
            return ProjectDataStatus.PENDING;
        }
        if (project.getClientId() == null) {
            return ProjectDataStatus.PENDING;
        }
        if (project.getProjectManagerId() == null) {
            return ProjectDataStatus.PENDING;
        }
        if (project.getStartDate() == null) {
            return ProjectDataStatus.PENDING;
        }
        return ProjectDataStatus.COMPLETE;
    }

    private boolean hasRelevantFieldChanges(Set<String> changedColumns) {
        return changedColumns.contains("name")
                || changedColumns.contains("client_id")
                || changedColumns.contains("project_manager_id")
                || changedColumns.contains("start_date")
                || changedColumns.contains("end_date");
    }
}
