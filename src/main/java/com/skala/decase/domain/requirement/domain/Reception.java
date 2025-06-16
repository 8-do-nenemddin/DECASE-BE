package com.skala.decase.domain.requirement.domain;

import com.skala.decase.domain.requirement.exception.DifficultyException;
import org.springframework.http.HttpStatus;

public enum Reception {
    ACCEPTED, UNACCEPTED;

    public static Reception fromKorean(String value) {
        return switch (value) {
            case "수용" -> ACCEPTED;
            case "미수용" -> UNACCEPTED;
            default -> throw new DifficultyException("Unknown difficulty value: " + value, HttpStatus.BAD_REQUEST);
        };
    }
}
