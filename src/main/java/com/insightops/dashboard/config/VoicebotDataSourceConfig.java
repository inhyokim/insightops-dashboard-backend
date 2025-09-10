package com.insightops.dashboard.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Voicebot DB 연결 설정
 * voc_raw 테이블에서 집계 데이터를 생성하기 위한 별도 DataSource
 */
@Configuration
public class VoicebotDataSourceConfig {

    @Value("${voicebot.datasource.url}")
    private String voicebotUrl;
    
    @Value("${voicebot.datasource.username}")
    private String voicebotUsername;
    
    @Value("${voicebot.datasource.password}")
    private String voicebotPassword;
    
    @Value("${voicebot.datasource.driver-class-name}")
    private String voicebotDriverClassName;

    @Bean(name = "voicebotDataSource")
    public DataSource voicebotDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(voicebotUrl);
        dataSource.setUsername(voicebotUsername);
        dataSource.setPassword(voicebotPassword);
        dataSource.setDriverClassName(voicebotDriverClassName);
        dataSource.setConnectionTimeout(60000);
        dataSource.setMaximumPoolSize(5);
        dataSource.setMinimumIdle(1);
        dataSource.setIdleTimeout(300000);
        return dataSource;
    }

    @Bean(name = "voicebotJdbcTemplate")
    public JdbcTemplate voicebotJdbcTemplate(@Qualifier("voicebotDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
