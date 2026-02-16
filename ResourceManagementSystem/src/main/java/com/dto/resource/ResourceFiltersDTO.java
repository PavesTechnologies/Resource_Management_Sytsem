package com.dto.resource;

import com.entity_enums.resource_enums.WorkforceCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceFiltersDTO {
    private List<String> location;
    private List<WorkforceCategory> workforceCategory;
    private List<String> designation;
    private Long maxExperience;
    private List<String> projectNames;
}
