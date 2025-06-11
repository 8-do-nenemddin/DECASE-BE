package com.skala.decase.global.config;

import jakarta.persistence.EntityManager;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditConfig {

    @Bean
    public AuditReader auditReader(EntityManager entityManager) {
        return AuditReaderFactory.get(entityManager);
    }
}
