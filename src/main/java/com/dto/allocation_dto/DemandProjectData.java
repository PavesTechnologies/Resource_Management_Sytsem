package com.dto.allocation_dto;

import com.entity.demand_entities.Demand;
import com.entity.project_entities.Project;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object for demand and project information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandProjectData {
    private Demand demand;
    private Project project;
}
