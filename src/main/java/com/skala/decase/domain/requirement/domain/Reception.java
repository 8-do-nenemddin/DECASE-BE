package com.skala.decase.domain.requirement.domain;

import com.skala.decase.domain.requirement.exception.DifficultyException;
import org.springframework.http.HttpStatus;

public enum Reception {
    ACCEPTED, UNACCEPTED;

    public static Reception fromKorean(String value) {
        return switch (value) {
            case "수용" -> ACCEPTED;
            case "미수용" -> UNACCEPTED;
            default -> throw new DifficultyException("Unknown Reception value: " + value, HttpStatus.BAD_REQUEST);
        };
    }

    public static String fromReception(String reception) {
        return switch (reception) {
            case "ACCEPTED" -> "수용";
            case "UNACCEPTED" -> "미수용";
            default -> throw new DifficultyException("Unknown Reception value: " + reception, HttpStatus.BAD_REQUEST);
        };
    }
}
