package com.example.blog.tag.service;

import com.example.blog.auth.service.AuthService;
import com.example.blog.common.exception.BadRequestException;
import com.example.blog.common.exception.ResourceNotFoundException;
import com.example.blog.tag.domain.Tag;
import com.example.blog.tag.dto.TagDtos.*;
import com.example.blog.tag.repository.TagRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Tag service for managing blog tags.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final AuthService authService;

    /**
     * Create a new tag.
     * Requires authentication.
     */
    @Transactional
    @CacheEvict(cacheNames = "tags", allEntries = true)
    public TagResponse createTag(CreateTagRequest request) {
        authService.getCurrentUserId(); // Ensure authenticated

        if (tagRepository.existsByName(request.getName())) {
            throw new BadRequestException("Tag with this name already exists");
        }

        // Generate slug from name
        String slug = generateSlug(request.getName());

        Tag tag = Tag.builder()
            .name(request.getName())
            .slug(slug)
            .build();

        Tag savedTag = tagRepository.save(tag);
        log.info("Tag created: {}", savedTag.getName());

        return mapToResponse(savedTag);
    }

    /**
     * Get all tags (cached).
     */
    @Cacheable(cacheNames = "tags")
    public List<TagResponse> getAllTags() {
        return tagRepository.findAll().stream()
            .map(this::mapToResponse)
            .toList();
    }

    /**
     * Get tag by ID.
     */
    public TagResponse getTagById(Long id) {
        Tag tag = tagRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tag", id.toString()));
        return mapToResponse(tag);
    }

    /**
     * Get tag by slug.
     */
    public TagResponse getTagBySlug(String slug) {
        Tag tag = tagRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Tag", "slug: " + slug));
        return mapToResponse(tag);
    }

    /**
     * Delete a tag (admin only).
     */
    @Transactional
    @CacheEvict(cacheNames = "tags", allEntries = true)
    public void deleteTag(Long id) {
        if (!authService.isAdmin()) {
            throw new com.example.blog.common.exception.AuthorizationException("Only admins can delete tags");
        }

        Tag tag = tagRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tag", id.toString()));

        tagRepository.delete(tag);
        log.info("Tag deleted: {}", tag.getName());
    }

    /**
     * Get popular tags (most used).
     * Could be enhanced with actual usage statistics.
     */
    public List<TagResponse> getPopularTags(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        // FIXME: This should order by post count, not createdAt
        return tagRepository.findAll(pageable).stream()
            .map(this::mapToResponse)
            .toList();
    }

    /**
     * Generate URL-friendly slug from tag name.
     */
    private String generateSlug(String name) {
        String baseSlug = name.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");

        String slug = baseSlug;
        int counter = 1;

        while (tagRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        return slug;
    }

    /**
     * Map Tag entity to TagResponse.
     */
    private TagResponse mapToResponse(Tag tag) {
        return new TagResponse(
            tag.getId(),
            tag.getName(),
            tag.getSlug(),
            tag.getCreatedAt(),
            tag.getPosts().size()
        );
    }
}
