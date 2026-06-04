package com.example.blog.common.exception;

/**
 * Thrown when a user tries to access/modify resources they don't have permission for.
 * Maps to HTTP 403.
 */
public class AuthorizationException extends BlogException {

    public AuthorizationException(String message) {
        super(message, "ACCESS_DENIED");
    }

    public AuthorizationException() {
        super("You don't have permission to perform this action", "ACCESS_DENIED");
    }
}
