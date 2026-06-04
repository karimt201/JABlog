package com.example.blog.auth.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Simple principal object for Spring Security.
 * Carries just enough info for authorization checks.
 */
@Data
@AllArgsConstructor
public class UserPrincipal {

    private final Long userId;
    private final String username;
    private final String role;
}
