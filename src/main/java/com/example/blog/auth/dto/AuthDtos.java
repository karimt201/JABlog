package com.example.blog.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication DTOs using Java records for immutability.
 * Clean and practical - no getter/setter boilerplate.
 */

/**
 * Register request DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;
}

/**
 * Login request DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Username or email is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}

/**
 * Refresh token request DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public record RefreshTokenRequest(
    String refreshToken
) {}

/**
 * Authentication response DTO - returned after successful login/register.
 */
public record AuthResponse(
    String accessToken,
    String refreshToken,
    UserDto user
) {}

/**
 * User DTO for auth responses.
 */
public record UserDto(
    Long id,
    String username,
    String email,
    String role
) {}
