package com.cdc.config.properties;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "cdc")
public class CdcProperties {

    private boolean enabled = true;

    @Min(1000)
    private long inboxPollIntervalMs = 5000L;

    private String snapshotMode = "when_needed";

    private String serverName = "pms_mysql";

    private String topicPrefix = "pms";

    private final Map<String, String> connector = new LinkedHashMap<>();

    private final DatabaseProperties database = new DatabaseProperties();

    private final TableProperties table = new TableProperties();

    private final FreshnessProperties freshness = new FreshnessProperties();

    private final CleanupProperties cleanup = new CleanupProperties();

    public String getConnectorClass() {
        return connector.getOrDefault("class", "io.debezium.connector.mysql.MySqlConnector");
    }

    public String getTableIncludeList() {
        return table.getInclude().getList();
    }

    @Getter
    @Setter
    public static class DatabaseProperties {

        private String type = "mysql";

        private String hostname;

        private Integer port;

        private String user;

        private String password;

        private String name;

        private final IncludeProperties include = new IncludeProperties();

        private final SslProperties ssl = new SslProperties();

        public String getIncludeList() {
            return include.getList();
        }
    }

    @Getter
    @Setter
    public static class IncludeProperties {

        private String list;
    }

    @Getter
    @Setter
    public static class TableProperties {

        private final IncludeProperties include = new IncludeProperties();
    }

    @Getter
    @Setter
    public static class SslProperties {

        private String mode = "required";
    }

    @Getter
    @Setter
    public static class FreshnessProperties {

        private boolean enabled = false;

        @Min(1)
        private int defaultThresholdMinutes = 60;

        private String timezone = "UTC";
    }

    @Getter
    @Setter
    public static class CleanupProperties {

        @Min(1)
        private int successRetentionDays = 30;

        @Min(1)
        private int cancelledRetentionDays = 7;

        @Min(1)
        private int deadLetterRetentionDays = 90;
    }
}
