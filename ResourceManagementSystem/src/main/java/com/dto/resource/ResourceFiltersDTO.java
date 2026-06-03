package com.dto.resource;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceFiltersDTO {
    private List<String> location;
    private List<String> designation;
    private Long maxExperience;
    private List<String> projectNames;
}
