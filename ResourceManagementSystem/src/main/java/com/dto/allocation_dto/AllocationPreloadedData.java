package com.dto.allocation_dto;

import com.entity.allocation_entities.ResourceAllocation;
import com.entity.resource_entities.Resource;
import com.entity.skill_entities.ResourceSkill;
import com.entity.skill_entities.ResourceCertificate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Data transfer object for preloaded allocation data to prevent N+1 queries
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllocationPreloadedData {
    private Map<Long, Resource> resourceMap;
    private Map<Long, List<ResourceAllocation>> allocationsByResource;
    private Map<Long, List<ResourceSkill>> skillsByResource;
    private Map<Long, List<ResourceCertificate>> certificatesByResource;
}
