package com.skala.decase.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DBInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DBInitializer.class);
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        System.out.println("[DEBUG] PostConstruct called");
        log.info("DB 이니셜라이저가 실행되었습니다.");
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            logger.info("Attempting to alter td_source table...");
            String sql = "ALTER TABLE td_source MODIFY COLUMN source_id BIGINT NOT NULL AUTO_INCREMENT;";
            jdbcTemplate.execute(sql);
            logger.info("Successfully altered td_source table: source_id is now AUTO_INCREMENT.");
            
            // AUTO_INCREMENT 설정 확인
            checkAutoIncrementSetting();
            
        } catch (Exception e) {
            logger.warn("Could not alter td_source table. This might be because it's already been altered or another issue occurred: {}", e.getMessage());
        }
    }
    
    private void checkAutoIncrementSetting() {
        try {
            // MariaDB/MySQL에서 컬럼 정보 조회
            String checkSql = "SHOW COLUMNS FROM td_source WHERE Field = 'source_id';";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(checkSql);
            
            if (!results.isEmpty()) {
                Map<String, Object> columnInfo = results.get(0);
                String extra = (String) columnInfo.get("Extra");
                String type = (String) columnInfo.get("Type");
                String nullable = (String) columnInfo.get("Null");
                
                logger.info("td_source.source_id column info:");
                logger.info("  - Type: {}", type);
                logger.info("  - Nullable: {}", nullable);
                logger.info("  - Extra: {}", extra);
                
                if (extra != null && extra.contains("auto_increment")) {
                    logger.info("✅ AUTO_INCREMENT is properly set on source_id column");
                } else {
                    logger.warn("⚠️ AUTO_INCREMENT is NOT set on source_id column");
                }
            } else {
                logger.warn("Could not find source_id column in td_source table");
            }
            
        } catch (Exception e) {
            logger.error("Error checking AUTO_INCREMENT setting: {}", e.getMessage());
        }
    }
} 