package com.example.blog.common.exception;

/**
 * Thrown for authentication failures (invalid credentials, tokens, etc).
 * Maps to HTTP 401.
 */
public class AuthenticationException extends BlogException {

    public AuthenticationException(String message) {
        super(message, "AUTHENTICATION_FAILED");
    }

    public AuthenticationException() {
        super("Invalid credentials", "AUTHENTICATION_FAILED");
    }
}
