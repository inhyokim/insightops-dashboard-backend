package com.insightops.dashboard.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * .env 파일을 로드하여 시스템 환경변수로 설정하는 Configuration 클래스
 */
@Configuration
public class DotenvConfig {
    
    private static final Logger log = LoggerFactory.getLogger(DotenvConfig.class);

    @PostConstruct
    public void loadDotenv() {
        try {
            // .env 파일 로드
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")  // 프로젝트 루트 디렉토리에서 .env 파일 찾기
                    .ignoreIfMalformed()  // 잘못된 형식의 라인 무시
                    .ignoreIfMissing()    // .env 파일이 없어도 에러 발생하지 않음
                    .load();

            // .env 파일의 변수들을 시스템 환경변수로 설정
            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                
                // 이미 시스템 환경변수로 설정되어 있지 않은 경우에만 설정
                if (System.getenv(key) == null) {
                    System.setProperty(key, value);
                    log.debug("Loaded environment variable from .env: {}={}", key, 
                            key.toLowerCase().contains("password") || key.toLowerCase().contains("key") ? "***" : value);
                } else {
                    log.debug("Environment variable {} already exists in system, skipping .env value", key);
                }
            });

            log.info("Successfully loaded .env file with {} variables", dotenv.entries().size());
            
        } catch (DotenvException e) {
            log.warn("Failed to load .env file: {}", e.getMessage());
            log.info("Continuing without .env file - using system environment variables and application.yml defaults");
        } catch (Exception e) {
            log.error("Unexpected error while loading .env file", e);
        }
    }
}
