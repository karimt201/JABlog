package com.example.blog.common.exception;

/**
 * Thrown when a requested resource doesn't exist or user doesn't have access.
 * Maps to HTTP 404 in the global handler.
 */
public class ResourceNotFoundException extends BlogException {

    public ResourceNotFoundException(String resource, String identifier) {
        super(String.format("%s not found with identifier: %s", resource, identifier), "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND");
    }
}
