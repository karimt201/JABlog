package com.example.blog.common.exception;

import java.util.Map;

/**
 * Thrown for client-side errors (validation failures, invalid input, etc).
 * Maps to HTTP 400.
 */
public class BadRequestException extends BlogException {

    private final Map<String, String> fieldErrors;

    public BadRequestException(String message) {
        super(message, "BAD_REQUEST");
        this.fieldErrors = null;
    }

    public BadRequestException(String message, Map<String, String> fieldErrors) {
        super(message, "VALIDATION_ERROR");
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
