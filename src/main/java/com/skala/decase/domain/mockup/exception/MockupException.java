package com.skala.decase.domain.mockup.exception;

import com.skala.decase.global.exception.CustomException;
import org.springframework.http.HttpStatus;

public class MockupException extends CustomException {
    public MockupException(String message, HttpStatus status) {
        super(message, status);
    }
}