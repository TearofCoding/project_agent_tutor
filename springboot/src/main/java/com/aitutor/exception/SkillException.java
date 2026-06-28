package com.aitutor.exception;

import org.springframework.http.HttpStatus;

public class SkillException extends DomainException {

    private SkillException(String message, HttpStatus status) {
        super(message, status);
    }

    public static SkillException notFound(String message) {
        return new SkillException(message, HttpStatus.NOT_FOUND);
    }

    public static SkillException badRequest(String message) {
        return new SkillException(message, HttpStatus.BAD_REQUEST);
    }

    public static SkillException badGateway(String message) {
        return new SkillException(message, HttpStatus.BAD_GATEWAY);
    }
}
