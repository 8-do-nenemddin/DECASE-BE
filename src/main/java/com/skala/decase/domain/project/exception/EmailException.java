package com.skala.decase.domain.project.exception;

import com.skala.decase.global.exception.CustomException;
import org.springframework.http.HttpStatus;

public class EmailException extends CustomException {
    public EmailException(String message, HttpStatus status) {
        super(message, status);
    }
}
