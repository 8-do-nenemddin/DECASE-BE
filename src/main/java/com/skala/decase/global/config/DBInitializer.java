package com.skala.decase.global.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DBInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DBInitializer.class);
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            logger.info("Attempting to alter TD_SOURCE table...");
            String sql = "ALTER TABLE TD_SOURCE MODIFY COLUMN source_id BIGINT NOT NULL AUTO_INCREMENT;";
            jdbcTemplate.execute(sql);
            logger.info("Successfully altered TD_SOURCE table: source_id is now AUTO_INCREMENT.");
        } catch (Exception e) {
            logger.warn("Could not alter TD_SOURCE table. This might be because it's already been altered or another issue occurred: {}", e.getMessage());
        }
    }
} 