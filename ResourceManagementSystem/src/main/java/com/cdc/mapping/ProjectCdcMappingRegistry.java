package com.cdc.mapping;

import com.entity_enums.project_enums.ProjectStage;
import com.entity_enums.project_enums.ProjectStatus;
import com.entity_enums.centralised_enums.DeliveryModel;
import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.centralised_enums.RiskLevel;

import java.util.HashMap;
import java.util.Map;

public class ProjectCdcMappingRegistry {

    public static final Map<String, ColumnMapping> PMS_TO_RMS = new HashMap<>();

    static {
        PMS_TO_RMS.put("name",
                new ColumnMapping("name", "name", FieldType.STRING, null));

        PMS_TO_RMS.put("client_id",
                new ColumnMapping("client_id", "clientId", FieldType.UUID, null));

        PMS_TO_RMS.put("owner_id",
                new ColumnMapping("owner_id", "projectManagerId", FieldType.LONG, null));

        PMS_TO_RMS.put("rm_id",
                new ColumnMapping("rm_id", "resourceManagerId", FieldType.LONG, null));

        PMS_TO_RMS.put("delivery_owner_id",
                new ColumnMapping("delivery_owner_id", "deliveryOwnerId", FieldType.LONG, null));

        PMS_TO_RMS.put("delivery_model",
                new ColumnMapping("delivery_model", "deliveryModel", FieldType.ENUM, DeliveryModel.class));

        PMS_TO_RMS.put("primary_location",
                new ColumnMapping("primary_location", "primaryLocation", FieldType.STRING, null));

        PMS_TO_RMS.put("status",
                new ColumnMapping("status", "projectStatus", FieldType.ENUM, ProjectStatus.class));

        PMS_TO_RMS.put("current_stage",
                new ColumnMapping("current_stage", "lifecycleStage", FieldType.ENUM, ProjectStage.class));

        PMS_TO_RMS.put("risk_level",
                new ColumnMapping("risk_level", "riskLevel", FieldType.ENUM, RiskLevel.class));

        PMS_TO_RMS.put("priority_level",
                new ColumnMapping("priority_level", "priorityLevel", FieldType.ENUM, PriorityLevel.class));

        PMS_TO_RMS.put("project_budget",
                new ColumnMapping("project_budget", "projectBudget", FieldType.BIG_DECIMAL, null));

        PMS_TO_RMS.put("project_budget_currency",
                new ColumnMapping("project_budget_currency", "projectBudgetCurrency", FieldType.STRING, null));

        PMS_TO_RMS.put("start_date",
                new ColumnMapping("start_date", "startDate", FieldType.LOCAL_DATE_TIME, null));

        PMS_TO_RMS.put("end_date",
                new ColumnMapping("end_date", "endDate", FieldType.LOCAL_DATE_TIME, null));

        PMS_TO_RMS.put("risk_level_updated_at",
                new ColumnMapping("risk_level_updated_at", "riskLevelUpdatedAt", FieldType.LOCAL_DATE_TIME, null));

    }

    private ProjectCdcMappingRegistry() {}
}
