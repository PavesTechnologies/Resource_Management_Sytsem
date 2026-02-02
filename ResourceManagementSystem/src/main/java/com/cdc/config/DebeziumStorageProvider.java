package com.cdc.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class DebeziumStorageProvider {

    private static final String BASE_DIR = "data/debezium";

    private String offsetFile;
    private String schemaHistoryFile;

    @PostConstruct
    public void init() {
        try {
            Path basePath = Paths.get(BASE_DIR);
            Files.createDirectories(basePath);

            this.offsetFile =
                    basePath.resolve("pms-project-offsets.dat")
                            .toAbsolutePath()
                            .toString();

            this.schemaHistoryFile =
                    basePath.resolve("pms-schema-history.dat")
                            .toAbsolutePath()
                            .toString();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to initialize Debezium storage", e);
        }
    }

    public String offsetFile() {
        return offsetFile;
    }

    public String schemaHistoryFile() {
        return schemaHistoryFile;
    }
}
