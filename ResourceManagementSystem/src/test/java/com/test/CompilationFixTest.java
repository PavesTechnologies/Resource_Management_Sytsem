package com.test;

import com.entity.skill_entities.ResourceSkill;
import com.entity.resource_entities.Resource;
import com.entity_enums.demand_enums.DemandCommitment;
import com.entity_enums.centralised_enums.PriorityLevel;

/**
 * Test compilation fixes
 */
public class CompilationFixTest {
    
    public static void main(String[] args) {
        System.out.println("ResourceSkill: " + ResourceSkill.class.getName());
        System.out.println("Resource: " + Resource.class.getName());
        System.out.println("DemandCommitment values:");
        for (DemandCommitment commitment : DemandCommitment.values()) {
            System.out.println("  " + commitment.name());
        }
        System.out.println("PriorityLevel values:");
        for (PriorityLevel priority : PriorityLevel.values()) {
            System.out.println("  " + priority.name());
        }
    }
}
