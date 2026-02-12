package com.service_imple.project_service_impl;

import com.entity.project_entities.Project;
import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.centralised_enums.RiskLevel;
import com.entity_enums.project_enums.ProjectStatus;
import com.entity_enums.project_enums.StaffingReadinessStatus;
import org.springframework.data.jpa.domain.Specification;

public class ProjectSpecification {

    public static Specification<Project> byManager(Long managerId) {
        return (root, query, cb) ->
                cb.equal(root.get("resourceManagerId"), managerId);
    }

    public static Specification<Project> search(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) return null;

            String like = "%" + search.toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("client").get("clientName")), like)
            );
        };
    }

    public static Specification<Project> readinessStatus(StaffingReadinessStatus status) {
        return (root, query, cb) ->
                status == null ? null :
                        cb.equal(root.get("staffingReadinessStatus"), status);
    }

    public static Specification<Project> projectStatus(ProjectStatus status) {
        return (root, query, cb) ->
                status == null ? null :
                        cb.equal(root.get("projectStatus"), status);
    }

    public static Specification<Project> priority(PriorityLevel priority) {
        return (root, query, cb) ->
                priority == null ? null :
                        cb.equal(root.get("priorityLevel"), priority);
    }

    public static Specification<Project> risk(RiskLevel risk) {
        return (root, query, cb) ->
                risk == null ? null :
                        cb.equal(root.get("riskLevel"), risk);
    }
}

