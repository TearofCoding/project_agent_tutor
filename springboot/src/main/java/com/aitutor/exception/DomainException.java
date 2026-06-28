package com.aitutor.exception;

import org.springframework.http.HttpStatus;

public class DomainException extends BaseException {

    public DomainException(String message, HttpStatus status) {
        super(message, status);
    }
}
