package com.example.blog.post.domain;

/**
 * Post publication status.
 * DRAFT posts are only visible to the author.
 * PUBLISHED posts are visible to everyone.
 */
public enum PostStatus {
    DRAFT,
    PUBLISHED
}
