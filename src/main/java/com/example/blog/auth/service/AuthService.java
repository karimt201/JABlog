package com.example.blog.auth.service;

import com.example.blog.auth.dto.AuthDtos.*;
import com.example.blog.auth.security.JwtService;
import com.example.blog.auth.security.UserPrincipal;
import com.example.blog.common.config.Constants;
import com.example.blog.common.exception.AuthenticationException;
import com.example.blog.common.exception.BadRequestException;
import com.example.blog.user.domain.User;
import com.example.blog.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Authentication service handling register, login, logout, and token refresh.
 * Uses Redis to store refresh tokens and logout blacklist.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final long REFRESH_TOKEN_CACHE_DAYS = 7;

    /**
     * Register a new user.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        // Create new user
        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role(com.example.blog.user.domain.Role.USER)
            .isActive(true)
            .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getUsername());

        // Generate tokens
        return generateTokens(user);
    }

    /**
     * Login user with username/password.
     */
    public AuthResponse login(LoginRequest request) {
        // Authenticate using Spring Security
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // Get user from DB
        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new AuthenticationException("User not found"));

        if (!user.getIsActive()) {
            throw new AuthenticationException("Account is disabled");
        }

        log.info("User logged in: {}", user.getUsername());
        return generateTokens(user);
    }

    /**
     * Logout user - blacklist the access token and remove refresh token from Redis.
     */
    public void logout(String accessToken) {
        // Remove token prefix to get raw token
        String rawToken = accessToken.replace("Bearer ", "");

        // Get current user
        UserPrincipal userPrincipal = getCurrentUser();
        if (userPrincipal == null) {
            throw new AuthenticationException("No authenticated user found");
        }

        // Blacklist the access token (until it expires naturally)
        // We'll extract expiration and store until then
        long expiration = jwtService.extractExpiration(rawToken).getTime() - System.currentTimeMillis();
        if (expiration > 0) {
            redisTemplate.opsForValue().set(
                Constants.REDIS_LOGOUT_TOKEN_PREFIX + rawToken,
                "true",
                expiration,
                TimeUnit.MILLISECONDS
            );
        }

        // Remove refresh token from Redis
        String refreshTokenKey = Constants.REDIS_REFRESH_TOKEN_PREFIX + userPrincipal.getUserId();
        redisTemplate.delete(refreshTokenKey);

        log.info("User logged out: {}", userPrincipal.getUsername());
    }

    /**
     * Refresh access token using refresh token.
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        // Validate refresh token
        if (!jwtService.isTokenValid(refreshToken)) {
            throw new AuthenticationException("Invalid refresh token");
        }

        // Extract userId from token
        Long userId = jwtService.extractUserId(refreshToken);

        // Check if refresh token exists in Redis (was issued by us)
        String refreshTokenKey = Constants.REDIS_REFRESH_TOKEN_PREFIX + userId;
        String storedToken = (String) redisTemplate.opsForValue().get(refreshTokenKey);

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new AuthenticationException("Refresh token not recognized or expired");
        }

        // Get user
        User user = userRepository.findActiveById(userId)
            .orElseThrow(() -> new AuthenticationException("User not found or inactive"));

        log.info("Token refreshed for user: {}", user.getUsername());
        return generateTokens(user);
    }

    /**
     * Get currently authenticated user from security context.
     */
    public UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }
        return null;
    }

    /**
     * Get current user ID or throw if not authenticated.
     */
    public Long getCurrentUserId() {
        UserPrincipal userPrincipal = getCurrentUser();
        if (userPrincipal == null) {
            throw new AuthenticationException("Not authenticated");
        }
        return userPrincipal.getUserId();
    }

    /**
     * Check if current user has ADMIN role.
     */
    public boolean isAdmin() {
        UserPrincipal userPrincipal = getCurrentUser();
        return userPrincipal != null && "ADMIN".equals(userPrincipal.getRole());
    }

    /**
     * Generate access and refresh tokens for user, store refresh token in Redis.
     */
    private AuthResponse generateTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getUsername());

        // Store refresh token in Redis
        String refreshTokenKey = Constants.REDIS_REFRESH_TOKEN_PREFIX + user.getId();
        redisTemplate.opsForValue().set(
            refreshTokenKey,
            refreshToken,
            Duration.ofDays(REFRESH_TOKEN_CACHE_DAYS)
        );

        UserDto userDto = new UserDto(user.getId(), user.getUsername(), user.getEmail(), user.getRole().toString());

        return new AuthResponse(accessToken, refreshToken, userDto);
    }
}
