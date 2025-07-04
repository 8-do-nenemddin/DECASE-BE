package com.skala.decase.domain.requirement.domain;

import com.skala.decase.domain.requirement.exception.DifficultyException;
import org.springframework.http.HttpStatus;

public enum Reception {
    ACCEPTED, REVIEWING, UNACCEPTED;

    public static Reception fromKorean(String reception) {
        return switch (reception) {
            case "ACCEPTED" -> ACCEPTED;
            case "REVIEWING" -> REVIEWING;
            case "UNACCEPTED" -> UNACCEPTED;
            default -> throw new DifficultyException("Unknown Reception value: " + reception, HttpStatus.BAD_REQUEST);
        };
    }

    public static String fromReception(String reception) {
        return switch (reception) {
            case "ACCEPTED" -> "수용";
            case "REVIEWING" -> "검토중";
            case "UNACCEPTED" -> "미수용";
            default -> throw new DifficultyException("Unknown Reception value: " + reception, HttpStatus.BAD_REQUEST);
        };
    }
}
