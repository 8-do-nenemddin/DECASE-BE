package com.skala.decase.domain.requirement.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.requirement.controller.dto.response.*;
import com.skala.decase.domain.requirement.domain.Difficulty;
import com.skala.decase.domain.requirement.domain.Priority;
import com.skala.decase.domain.requirement.domain.Requirement;
import com.skala.decase.domain.requirement.domain.RequirementType;
import com.skala.decase.domain.source.domain.Source;
import lombok.AllArgsConstructor;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.springframework.stereotype.Component;

import java.security.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@AllArgsConstructor
public class RequirementAuditMapper {

    private final ObjectMapper objectMapper;

    public RequirementAuditDTO toEntity(Object[] result) {
        Requirement requirement = (Requirement) result[0];
        DefaultRevisionEntity revisionEntity = (DefaultRevisionEntity) result[1];
        RevisionType revisionType = (RevisionType) result[2];

        return new RequirementAuditDTO(
                requirement,
                revisionEntity.getId(),
                revisionEntity.getRevisionDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                revisionType
        );
    }

    public RequirementResponse toDtoResponse(Object[] result, int revisionCount) {
        return new RequirementResponse(
                (Long) result[0],
                (String) result[1],
                revisionCount,
                (String) result[2],
                (String) result[13],
                (String) result[3],
                (String) result[4],
                (String) result[5],
                (String) result[8],
                (String) result[9],
                (String) result[6],
                (String) result[7],
                mapRevtypeToString(result[12]),
                getLocalDateTime(result[11]),
                getLocalDateTime(result[10]),
                null,
                null //parseSourcesForResponse(result[14])
        );
    }

    private String mapRevtypeToString(Object revType) {
        if (revType == null) return null;

        int value;
        if (revType instanceof Number) {
            value = ((Number) revType).intValue();  // Byte, Integer, etc. 모두 처리
        } else {
            return "알 수 없음";
        }

        return switch (value) {
            case 0 -> "추가";
            case 1 -> "수정";
            case 2 -> "삭제";
            default -> "알 수 없음";
        };
    }

    private List<SourceResponse> parseSourcesForResponse(Object jsonValue) {
        if (jsonValue == null) return new ArrayList<>();

        String jsonString;
        if (jsonValue instanceof String str) {
            jsonString = str;
        } else {
            jsonString = String.valueOf(jsonValue); // 최후의 수단
        }

        if (jsonString.trim().isEmpty() || "[]".equals(jsonString.trim())) {
            return new ArrayList<>();
        }

        try {
            List<Map<String, Object>> sourcesList = objectMapper.readValue(
                    jsonString,
                    new TypeReference<>() {}
            );
            return sourcesList.stream()
                    .map(this::mapToSourceResponse)
                    .toList();
        } catch (Exception e) {
            System.err.println("❌ Failed to parse sources JSON: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private SourceResponse mapToSourceResponse(Map<String, Object> sourceMap) {
        Number sourceIdRaw = (Number) sourceMap.get("source_id");
        Number pageNumRaw = (Number) sourceMap.get("page_num");

        Long sourceId = sourceIdRaw != null ? sourceIdRaw.longValue() : null;
        Integer pageNum = pageNumRaw != null ? pageNumRaw.intValue() : null;

        return new SourceResponse(
                sourceId,
                (String) sourceMap.get("doc_id"),
                (String) sourceMap.get("doc_name"),
                pageNum,
                (String) sourceMap.get("rel_sentence")
        );
    }

    private LocalDateTime getLocalDateTime(Object value) {
        if (value == null) return null;

        if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) value).toLocalDateTime();
        } else if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate().atStartOfDay();
        } else if (value instanceof java.util.Date) {
            return ((java.util.Date) value).toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        } else if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        } else if (value instanceof LocalDate) {
            return ((LocalDate) value).atStartOfDay();
        }

        throw new IllegalArgumentException("Unsupported type for date conversion: " + value.getClass());
    }

    public RequirementModReasonResponse toModReasonResponse(Object[] result) {
        String reqIdCode = (String) result[0];
        String modReason = (String) result[1];
        Object revTimestampObj = result[2];

        LocalDateTime revisionDate;
        if (revTimestampObj instanceof java.sql.Timestamp) {
            revisionDate = ((java.sql.Timestamp) revTimestampObj).toLocalDateTime();
        } else if (revTimestampObj instanceof Long) {
            revisionDate = Instant.ofEpochMilli((Long) revTimestampObj).atZone(ZoneId.systemDefault()).toLocalDateTime();
        } else if (revTimestampObj instanceof LocalDateTime) {
            revisionDate = (LocalDateTime) revTimestampObj;
        } else {
            throw new IllegalArgumentException("Unsupported revTimestamp type: " + revTimestampObj.getClass());
        }

        return new RequirementModReasonResponse(reqIdCode, modReason, revisionDate);
    }


    public RequirementAuditResponse toResponse(RequirementAuditDTO requirementAuditDTO) {
        return new RequirementAuditResponse(
                requirementAuditDTO.getRevisionNumber(),
                requirementAuditDTO.getRequirement().getRevisionCount(),
                requirementAuditDTO.getRequirement().getReqIdCode(),
                requirementAuditDTO.getRequirement().getType(),
                requirementAuditDTO.getRequirement().getName(),
                requirementAuditDTO.getRequirement().getDescription(),
                requirementAuditDTO.getRequirement().getLevel1(),
                requirementAuditDTO.getRequirement().getLevel2(),
                requirementAuditDTO.getRequirement().getLevel3(),
                requirementAuditDTO.getRequirement().getPriority(),
                requirementAuditDTO.getRequirement().getDifficulty(),
                requirementAuditDTO.getRequirement().getReception(),
                requirementAuditDTO.getRequirement().getModReason(),
                requirementAuditDTO.getRevisionDate(),
                requirementAuditDTO.getRequirement().getCreatedBy().getId(),
                requirementAuditDTO.getRequirement().getCreatedBy().getName(),
                requirementAuditDTO.getRevisionType().toString()
        );
    }
}
