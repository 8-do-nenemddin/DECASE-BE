package com.skala.decase.domain.requirement.exception;

import com.skala.decase.global.exception.CustomException;
import org.springframework.http.HttpStatus;

public class PendingRequirementException extends CustomException {
    public PendingRequirementException(String message, HttpStatus status) {
        super(message, status);
    }
}
