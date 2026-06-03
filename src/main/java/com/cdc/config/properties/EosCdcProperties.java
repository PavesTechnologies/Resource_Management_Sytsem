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
@ConfigurationProperties(prefix = "eos.cdc")
public class EosCdcProperties {

    private String baseDirectory;

    private String snapshotMode;

    private String serverName = "eos_mysql";

    private String topicPrefix = "eos";

    private final Map<String, String> connector = new LinkedHashMap<>();

    private final DatabaseProperties database = new DatabaseProperties();

    private final TableProperties table = new TableProperties();

    public String getConnectorClass() {
        return connector.getOrDefault("class", "io.debezium.connector.mysql.MySqlConnector");
    }

    public String getConnectorName() {
        return connector.getOrDefault("name", "eos-cdc");
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
}
