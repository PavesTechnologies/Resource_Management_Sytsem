package com.cdc.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class EosResyncDataSourceConfig {

    @Value("${eos.cdc.database.hostname}")
    private String hostname;

    @Value("${eos.cdc.database.port}")
    private int port;

    @Value("${eos.cdc.database.user}")
    private String user;

    @Value("${eos.cdc.database.password}")
    private String password;

    @Value("${eos.cdc.database.name}")
    private String database;

    // DataSource is NOT exposed as a Spring bean so Hibernate ddl-auto never
    // discovers it and tries to run CREATE TABLE against the EOS read-only DB.
    @Bean("eosJdbcTemplate")
    public JdbcTemplate eosJdbcTemplate() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=true&allowPublicKeyRetrieval=true",
                hostname, port, database));
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(3);
        config.setConnectionTimeout(30000);
        config.setReadOnly(true);
        config.setPoolName("eos-resync-pool");
        return new JdbcTemplate(new HikariDataSource(config));
    }
}
