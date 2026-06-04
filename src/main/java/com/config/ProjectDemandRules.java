package com.config;

import com.entity_enums.project_enums.ProjectStage;
import com.entity_enums.project_enums.ProjectStatus;

import java.util.EnumSet;
import java.util.Set;

public class ProjectDemandRules {
    public static final Set<ProjectStatus> ALLOWED_PROJECT_STATUSES =
            EnumSet.of(ProjectStatus.ACTIVE);

    // STORY 8
    public static final Set<ProjectStage> ALLOWED_LIFECYCLE_STAGES =
            EnumSet.of(
                    ProjectStage.DESIGN,
                    ProjectStage.DEVELOPMENT,
                    ProjectStage.TESTING,
                    ProjectStage.DEPLOYMENT
            );
}
