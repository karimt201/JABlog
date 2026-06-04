package com.example.blog.post.dto;

import com.example.blog.post.domain.PostStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Post DTOs - separate from entities to avoid exposing internal structure.
 * Using records for response DTOs (immutable), classes for requests (mutable for validation).
 */

/**
 * Create post request DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    @Builder.Default
    private PostStatus status = PostStatus.DRAFT;

    private Set<String> tagNames; // Tag names, will be converted to Tag entities
    private String featuredImage;
}

/**
 * Update post request DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    private PostStatus status;
    private Set<String> tagNames;
    private String featuredImage;
}

/**
 * Post response DTO - returned to clients.
 */
public record PostResponse(
    Long id,
    String title,
    String slug,
    String content,
    PostStatus status,
    String featuredImage,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    AuthorInfo author,
    Set<TagDto> tags,
    int commentCount
) {
    public record AuthorInfo(
        Long id,
        String username
    ) {}

    public record TagDto(
        Long id,
        String name,
        String slug
    ) {}
}

/**
 * Post list response DTO (for paginated lists, lighter than full PostResponse).
 */
public record PostListItem(
    Long id,
    String title,
    String slug,
    String excerpt,
    PostStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    AuthorInfo author,
    Set<TagDto> tags
) {
    public record AuthorInfo(
        Long id,
        String username
    ) {}

    public record TagDto(
        Long id,
        String name,
        String slug
    ) {}
}

/**
 * Publish/unpublish request DTO.
 */
public record PublishRequest(
    PostStatus status
) {}
