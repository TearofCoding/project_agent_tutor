package com.aitutor.exception;

import org.springframework.http.HttpStatus;

public class UserException extends DomainException {

    public UserException(String message, HttpStatus status) {
        super(message, status);
    }

    public static UserException badRequest(String message) {
        return new UserException(message, HttpStatus.BAD_REQUEST);
    }

    public static UserException unauthorized(String message) {
        return new UserException(message, HttpStatus.UNAUTHORIZED);
    }

    public static UserException forbidden(String message) {
        return new UserException(message, HttpStatus.FORBIDDEN);
    }

    public static UserException notFound(String message) {
        return new UserException(message, HttpStatus.NOT_FOUND);
    }

    public static UserException conflict(String message) {
        return new UserException(message, HttpStatus.CONFLICT);
    }
}
