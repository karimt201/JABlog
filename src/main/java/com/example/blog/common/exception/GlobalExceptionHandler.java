package com.example.blog.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler - catches all exceptions and returns consistent API responses.
 * This is where we translate exceptions into proper HTTP responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BlogException.class)
    public ResponseEntity<ApiResponse<Void>> handleBlogException(BlogException ex) {
        log.warn("BlogException: {}", ex.getMessage());

        HttpStatus status = switch (ex.getClass().getSimpleName()) {
            case "ResourceNotFoundException" -> HttpStatus.NOT_FOUND;
            case "AuthenticationException" -> HttpStatus.UNAUTHORIZED;
            case "AuthorizationException" -> HttpStatus.FORBIDDEN;
            case "BadRequestException" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        ApiResponse<Void> response = ((BadRequestException) ex).getFieldErrors() != null
            ? ApiResponse.error(ex.getMessage(), ((BadRequestException) ex).getErrorCode(), ((BadRequestException) ex).getFieldErrors())
            : ApiResponse.error(ex.getMessage(), ex.getErrorCode());

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                (existing, replacement) -> existing + "; " + replacement
            ));

        log.warn("Validation failed: {}", errors);
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Validation failed", "VALIDATION_ERROR", errors));
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials attempt");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("Invalid username or password", "INVALID_CREDENTIALS"));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("An unexpected error occurred. Please try again.", "INTERNAL_ERROR"));
    }
}
