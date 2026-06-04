package com.example.blog.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tag DTOs.
 */

/**
 * Create tag request DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTagRequest {

    @NotBlank(message = "Tag name is required")
    @Size(max = 50, message = "Tag name must not exceed 50 characters")
    private String name;
}

/**
 * Tag response DTO.
 */
public record TagResponse(
    Long id,
    String name,
    String slug,
    LocalDateTime createdAt,
    int postCount
) {}
