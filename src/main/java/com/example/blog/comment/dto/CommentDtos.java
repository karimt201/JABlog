package com.example.blog.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Comment DTOs.
 */

/**
 * Create comment request DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {

    @NotBlank(message = "Comment body is required")
    @Size(max = 2000, message = "Comment must not exceed 2000 characters")
    private String body;
}

/**
 * Comment response DTO.
 */
public record CommentResponse(
    Long id,
    String body,
    LocalDateTime createdAt,
    AuthorInfo author
) {
    public record AuthorInfo(
        Long id,
        String username
    ) {}
}
