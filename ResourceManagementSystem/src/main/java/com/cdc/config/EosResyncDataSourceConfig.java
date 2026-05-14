package com.cdc.config;

import com.cdc.config.properties.EosCdcProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@RequiredArgsConstructor
public class EosResyncDataSourceConfig {
    private final EosCdcProperties eosCdcProperties;

    // DataSource is NOT exposed as a Spring bean so Hibernate ddl-auto never
    // discovers it and tries to run CREATE TABLE against the EOS read-only DB.
    @Bean("eosJdbcTemplate")
    public JdbcTemplate eosJdbcTemplate() {
        EosCdcProperties.DatabaseProperties database = eosCdcProperties.getDatabase();
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=true&allowPublicKeyRetrieval=true",
                database.getHostname(), database.getPort(), database.getName()));
        config.setUsername(database.getUser());
        config.setPassword(database.getPassword());
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(3);
        config.setConnectionTimeout(30000);
        config.setReadOnly(true);
        config.setPoolName("eos-resync-pool");
        return new JdbcTemplate(new HikariDataSource(config));
    }
}
