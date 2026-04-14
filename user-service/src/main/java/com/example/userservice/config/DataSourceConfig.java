package com.example.userservice.config;

import com.example.userservice.datasource.DynamicDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String masterUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${spring.datasource.slave.url:#{null}}")
    private String slaveUrl;

    @Bean
    @Primary
    public DataSource masterDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(masterUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(20000);
        config.setPoolName("master-pool");
        return new HikariDataSource(config);
    }

    @Bean(name = "slaveDataSource")
    public DataSource slaveDataSource() {
        HikariConfig config = new HikariConfig();
        if (slaveUrl != null && !slaveUrl.isBlank()) {
            config.setJdbcUrl(slaveUrl);
        } else {
            config.setJdbcUrl(masterUrl);
        }
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(20000);
        config.setPoolName("slave-pool");
        return new HikariDataSource(config);
    }

    @Bean
    @Primary
    public DataSource dynamicDataSource(DataSource masterDataSource, DataSource slaveDataSource) {
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("master", masterDataSource);
        targetDataSources.put("slave", slaveDataSource);

        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.setDefaultTargetDataSource(masterDataSource);
        return dynamicDataSource;
    }
}
