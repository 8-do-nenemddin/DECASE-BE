package com.skala.decase.global;

import com.skala.decase.global.config.DBInitializer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Transactional
@Service
@RequiredArgsConstructor
public class DataInitService {

    private static final Logger logger = LoggerFactory.getLogger(DataInitService.class);
    private final JdbcTemplate jdbcTemplate;

    public void insertCompany() {
        try {
            logger.info("🏢 회사 데이터 확인 중...");
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tm_companies", Integer.class);
            if (Objects.requireNonNull(count).equals(0)) {
                logger.info("📝 회사 테이블이 비어있음. 초기 데이터 삽입 중...");

                String[] companies = {"SK", "SK AX", "SK ON", "SK Hynix", "ATS"};
                for (String company : companies) {
                    jdbcTemplate.update("INSERT INTO `tm_companies` (name) VALUES (?)", company);
                    logger.info("  - 회사 추가: {}", company);
                }

                logger.info("✅ 회사 데이터 삽입 완료 ({} 개)", companies.length);
            } else {
                logger.info("✅ 회사 데이터가 이미 존재함 ({} 개)", count);
            }
        } catch (Exception e) {
            logger.error("❌ 회사 데이터 삽입 중 오류: {}", e.getMessage());
        }
    }

    public void insertDepartment() {
        logger.info("🏛️ 부서 데이터 확인 중...");
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `tn_departments`", Integer.class);

        if (Objects.requireNonNull(count).equals(0)) {
            logger.info("📝 부서 테이블이 비어있음. 초기 데이터 삽입 중...");

            // 회사별 부서 데이터 정의
            Map<String, String[]> departmentsByCompany = new HashMap<>();
            departmentsByCompany.put("SK", new String[]{"전략 기획팀", "기술 혁신팀", "윤리 경영팀"});
            departmentsByCompany.put("SK AX", new String[]{"운영팀", "R&D 혁신팀", "클라우드 인프라팀", "시스템 통합(SI)팀", "AI 혁신팀", "AI 솔루션 개발팀"});
            departmentsByCompany.put("SK ON", new String[]{"배터리 연구소", "생산팀"});
            departmentsByCompany.put("SK Hynix", new String[]{"DRAM 개발팀", "NAND 솔루션팀", "글로벌 제조 운영팀"});
            departmentsByCompany.put("ATS", new String[]{"프로덕트팀", "서비스 개발팀", "sw아키텍처 설계팀"});

            int totalDepartments = 0;
            for (Map.Entry<String, String[]> entry : departmentsByCompany.entrySet()) {
                String companyName = entry.getKey();
                String[] departments = entry.getValue();
                Long companyId = findCompanyIdByName(companyName).orElse(null);

                if (companyId != null) {
                    for (String departmentName : departments) {
                        jdbcTemplate.update("INSERT INTO `tn_departments` (company_id, name) VALUES (?, ?)",
                                companyId, departmentName);
                        logger.info("  - 부서 추가: {} (회사: {}, ID: {})", departmentName, companyName, companyId);
                        totalDepartments++;
                    }
                } else {
                    logger.warn("⚠️ 회사를 찾을 수 없음: {}", companyName);
                }
            }

            logger.info("✅ 부서 데이터 삽입 완료 ({} 개)", totalDepartments);
        } else {
            logger.info("✅ 부서 데이터가 이미 존재함 ({} 개)", count);
        }
    }

    /**
     * 회사 테이블에서 실제 company_id 조회
     */
    public Optional<Long> findCompanyIdByName(String companyName) {
        String sql = "SELECT company_id FROM tm_companies WHERE name = ?";
        try {
            Long companyId = jdbcTemplate.queryForObject(sql, Long.class, companyName);
            return Optional.ofNullable(companyId);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
