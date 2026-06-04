package com.config;

import com.entity.skill_entities.ProficiencyLevel;
import com.repo.skill_repo.ProficiencyLevelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor

public class MasterDataInitializer implements ApplicationRunner {
    private final ProficiencyLevelRepository proficiencyLevelRepository;

    @Override
    public void run(ApplicationArguments args) {
        log.info("==============Initializing master data...========================");

        seed(
                "L-1",
                "BEGINNER",
                "Beginner",
                1
        );

        seed(
                "L-2",
                "INTERMEDIATE",
                "Intermediate",
                2
        );

        seed(
                "L-3",
                "EXPERT",
                "Expert",
                3
        );

        seed(
                "L-4",
                "ADVANCED",
                "Advanced",
                4
        );
        log.info("==============Master data initialized successfully================");
    }

    private void seed(
            String code,
            String name,
            String description,
            Integer displayOrder
    ) {

        if (proficiencyLevelRepository
                .existsByProficiencyCodeIgnoreCase(code)) {
            log.info("Proficiency already exists: {}", code);
            return;
        }

        proficiencyLevelRepository.save(
                ProficiencyLevel.builder()
                        .proficiencyCode(code)
                        .proficiencyName(name)
                        .description(description)
                        .displayOrder(displayOrder)
                        .activeFlag(true)
                        .build()
        );
    }

}
