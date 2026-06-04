package com.example.blog.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard API response envelope for all endpoints.
 * Keeps the API contract consistent and makes frontend handling easier.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final ApiError error;
    private final LocalDateTime timestamp;

    private ApiResponse(boolean success, String message, T data, ApiError error) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.error = error;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Operation successful", data, null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, "Resource created successfully", data, null);
    }

    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>(false, message, null, new ApiError(errorCode, message));
    }

    public static <T> ApiResponse<T> error(String message, String errorCode, Map<String, String> details) {
        return new ApiResponse<>(false, message, null, new ApiError(errorCode, message, details));
    }

    @Getter
    @AllArgsConstructor
    public static class ApiError {
        private final String code;
        private final String message;
        private final Map<String, String> details;
    }
}
