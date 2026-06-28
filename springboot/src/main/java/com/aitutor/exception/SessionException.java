package com.aitutor.exception;

import org.springframework.http.HttpStatus;

public class SessionException extends DomainException {

    private SessionException(String message, HttpStatus status) {
        super(message, status);
    }

    public static SessionException notFound(String message) {
        return new SessionException(message, HttpStatus.NOT_FOUND);
    }

    public static SessionException badRequest(String message) {
        return new SessionException(message, HttpStatus.BAD_REQUEST);
    }

    public static SessionException forbidden(String message) {
        return new SessionException(message, HttpStatus.FORBIDDEN);
    }
}
