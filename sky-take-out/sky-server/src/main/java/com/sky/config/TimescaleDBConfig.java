package com.sky.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * TimescaleDB 时序库数据源配置（第二数据源）
 *
 * 注意：主数据源（smart_logistics 业务库）由 Spring Boot + Druid 自动装配。
 * 这里仅配置 TimescaleDB（gps 库），通过 @Qualifier 区分。
 */
@Configuration
@Slf4j
public class TimescaleDBConfig {

    /**
     * TimescaleDB 数据源，读取 timescaledb.datasource.* 配置
     */
    @Bean(name = "timescaleDataSource")
    @ConfigurationProperties(prefix = "timescaledb.datasource")
    public DataSource timescaleDataSource() {
        log.info("初始化 TimescaleDB 数据源...");
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    /**
     * TimescaleDB 专用 JdbcTemplate
     */
    @Bean(name = "timescaleJdbcTemplate")
    public JdbcTemplate timescaleJdbcTemplate(
            @Qualifier("timescaleDataSource") DataSource dataSource) {
        log.info("初始化 TimescaleDB JdbcTemplate...");
        return new JdbcTemplate(dataSource);
    }
}
