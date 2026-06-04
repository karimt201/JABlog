package com.example.blog.common.exception;

import lombok.Getter;

/**
 * Base exception for all application-specific errors.
 * This gives us control over error codes and messages.
 */
@Getter
public class BlogException extends RuntimeException {

    private final String errorCode;

    public BlogException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BlogException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
