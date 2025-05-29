package com.skala.decase.domain.document.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class DocumentException extends RuntimeException {
    private final HttpStatus status;

    public DocumentException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}