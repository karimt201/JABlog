package com.example.blog.common.config;

/**
 * Application-wide constants.
 * Keeps magic numbers/strings in one place.
 */
public final class Constants {

    private Constants() {}

    // Cache Keys
    public static final String CACHE_POSTS = "posts";
    public static final String CACHE_TAGS = "tags";

    // Redis Keys for auth
    public static final String REDIS_REFRESH_TOKEN_PREFIX = "refresh_token:";
    public static final String REDIS_LOGOUT_TOKEN_PREFIX = "logout_token:";

    // Pagination Defaults
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // JWT Claim Keys
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_ROLE = "role";

    // Date Format
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
}
