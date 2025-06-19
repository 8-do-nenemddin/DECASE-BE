package com.skala.decase.domain.requirement.domain;

import com.skala.decase.domain.requirement.exception.RequirementTypeException;
import org.springframework.http.HttpStatus;

public enum RequirementType {
    FR, NFR;

    public static RequirementType fromKorean(String value) {
        if (value == null) {
            return null; // 또는 기본값 반환
        }

        return switch (value) {
            case "기능" -> FR;
            case "비기능" -> NFR;
            default -> throw new RequirementTypeException("Unknown requirement type value: " + value,
                    HttpStatus.BAD_REQUEST);
        };
    }

    public static String toKorean(String type) {
        if ("FR".equals(type)) {
            return "기능";
        } else if ("NFR".equals(type)) {
            return "비기능";
        } else {
            return type;
        }
    }

    public static RequirementType fromOrdinal(int ordinal) {
        return switch (ordinal) {
            case 0 -> FR;
            case 1 -> NFR;
            default -> throw new RequirementTypeException("Unknown requirement type ordinal: " + ordinal,
                    HttpStatus.BAD_REQUEST);
        };
    }
}
