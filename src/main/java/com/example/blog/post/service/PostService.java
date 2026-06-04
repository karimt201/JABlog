package com.example.blog.post.service;

import com.example.blog.auth.service.AuthService;
import com.example.blog.common.exception.AuthorizationException;
import com.example.blog.common.exception.ResourceNotFoundException;
import com.example.blog.post.domain.Post;
import com.example.blog.post.domain.PostStatus;
import com.example.blog.post.dto.PostDtos.*;
import com.example.blog.post.repository.PostRepository;
import com.example.blog.tag.domain.Tag;
import com.example.blog.tag.repository.TagRepository;
import com.example.blog.user.domain.User;
import com.example.blog.user.repository.UserRepository;
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

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Post service handling CRUD operations, filtering, and pagination.
 * Uses caching for frequently accessed data (published posts).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final AuthService authService;

    private static final int MAX_EXCERPT_LENGTH = 200;

    /**
     * Create a new post.
     * Tags are created if they don't exist, or linked if they do.
     */
    @Transactional
    @CacheEvict(cacheNames = "posts", allEntries = true)
    public PostResponse createPost(CreatePostRequest request) {
        Long currentUserId = authService.getCurrentUserId();

        User author = userRepository.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId.toString()));

        // Generate slug from title (simple approach - could be improved)
        String slug = generateSlug(request.getTitle());

        Post post = Post.builder()
            .title(request.getTitle())
            .slug(slug)
            .content(request.getContent())
            .status(request.getStatus() != null ? request.getStatus() : PostStatus.DRAFT)
            .featuredImage(request.getFeaturedImage())
            .author(author)
            .build();

        // Handle tags
        if (request.getTagNames() != null && !request.getTagNames().isEmpty()) {
            Set<Tag> tags = resolveTags(request.getTagNames());
            tags.forEach(post::addTag);
        }

        Post savedPost = postRepository.save(post);
        log.info("Post created: {} by user: {}", savedPost.getSlug(), author.getUsername());

        return mapToResponse(savedPost);
    }

    /**
     * Update an existing post.
     * Only the author can update their own posts.
     */
    @Transactional
    @CacheEvict(cacheNames = "posts", allEntries = true)
    public PostResponse updatePost(Long postId, UpdatePostRequest request) {
        Long currentUserId = authService.getCurrentUserId();

        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", postId.toString()));

        // Check ownership
        if (!post.isAuthor(currentUserId) && !authService.isAdmin()) {
            throw new AuthorizationException("You can only update your own posts");
        }

        // Update fields
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setFeaturedImage(request.getFeaturedImage());

        if (request.getStatus() != null) {
            post.setStatus(request.getStatus());
        }

        // Handle tags - replace existing tags with new ones
        if (request.getTagNames() != null) {
            // Remove existing tags
            new HashSet<>(post.getTags()).forEach(post::removeTag);
            // Add new tags
            Set<Tag> tags = resolveTags(request.getTagNames());
            tags.forEach(post::addTag);
        }

        Post updatedPost = postRepository.save(post);
        log.info("Post updated: {}", updatedPost.getSlug());

        return mapToResponse(updatedPost);
    }

    /**
     * Get post by ID.
     * Draft posts are only accessible to the author.
     */
    public PostResponse getPostById(Long postId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", postId.toString()));

        // Check access for draft posts
        if (post.getStatus() == PostStatus.DRAFT) {
            Long currentUserId = authService.getCurrentUserId();
            if (!post.isAuthor(currentUserId) && !authService.isAdmin()) {
                throw new AuthorizationException("You don't have access to this draft post");
            }
        }

        return mapToResponse(post);
    }

    /**
     * Get post by slug (public view).
     * Only published posts are accessible.
     */
    @Cacheable(cacheNames = "posts", key = "#slug")
    public PostResponse getPostBySlug(String slug) {
        Post post = postRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "slug: " + slug));

        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new AuthorizationException("This post is not published");
        }

        return mapToResponse(post);
    }

    /**
     * Delete a post.
     * Only the author can delete their own posts.
     */
    @Transactional
    @CacheEvict(cacheNames = "posts", allEntries = true)
    public void deletePost(Long postId) {
        Long currentUserId = authService.getCurrentUserId();

        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", postId.toString()));

        if (!post.isAuthor(currentUserId) && !authService.isAdmin()) {
            throw new AuthorizationException("You can only delete your own posts");
        }

        postRepository.delete(post);
        log.info("Post deleted: {}", post.getSlug());
    }

    /**
     * Get published posts with pagination.
     */
    @Cacheable(cacheNames = "posts", key = "'published:' + #page + ':' + #size")
    public Page<PostListItem> getPublishedPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return postRepository.findByStatus(PostStatus.PUBLISHED, pageable)
            .map(this::mapToListItem);
    }

    /**
     * Get posts by current user (including drafts).
     */
    public Page<PostListItem> getMyPosts(int page, int size) {
        Long currentUserId = authService.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return postRepository.findByAuthorId(currentUserId, pageable)
            .map(this::mapToListItem);
    }

    /**
     * Get posts by author (public view - only published).
     */
    @Cacheable(cacheNames = "posts", key = "'author:' + #authorId + ':' + #page + ':' + #size")
    public Page<PostListItem> getPostsByAuthor(Long authorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return postRepository.findByAuthorIdAndStatus(authorId, PostStatus.PUBLISHED, pageable)
            .map(this::mapToListItem);
    }

    /**
     * Get posts by tag (only published).
     */
    @Cacheable(cacheNames = "posts", key = "'tag:' + #tagSlug + ':' + #page + ':' + #size")
    public Page<PostListItem> getPostsByTag(String tagSlug, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return postRepository.findByTagSlugAndStatus(tagSlug, PostStatus.PUBLISHED, pageable)
            .map(this::mapToListItem);
    }

    /**
     * Search posts by keyword (only published).
     */
    public Page<PostListItem> searchPosts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return postRepository.searchByKeyword(keyword, PostStatus.PUBLISHED, pageable)
            .map(this::mapToListItem);
    }

    /**
     * Filter posts with multiple criteria.
     */
    @Cacheable(cacheNames = "posts", key = "'filter:' + #status + ':' + #authorId + ':' + #tagSlug + ':' + #page + ':' + #size")
    public Page<PostListItem> filterPosts(PostStatus status, Long authorId, String tagSlug, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // If filtering for draft posts, require authentication and ownership check
        if (status == PostStatus.DRAFT) {
            authService.getCurrentUserId(); // Throws if not authenticated
        }

        return postRepository.filterPosts(status, authorId, tagSlug, pageable)
            .map(this::mapToListItem);
    }

    /**
     * Publish or unpublish a post.
     */
    @Transactional
    @CacheEvict(cacheNames = "posts", allEntries = true)
    public PostResponse setPostStatus(Long postId, PostStatus status) {
        Long currentUserId = authService.getCurrentUserId();

        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", postId.toString()));

        if (!post.isAuthor(currentUserId) && !authService.isAdmin()) {
            throw new AuthorizationException("You can only change status of your own posts");
        }

        post.setStatus(status);
        Post savedPost = postRepository.save(post);
        log.info("Post {} status changed to {}", savedPost.getSlug(), status);

        return mapToResponse(savedPost);
    }

    /**
     * Helper to resolve tag names to Tag entities (create if not exists).
     */
    private Set<Tag> resolveTags(Set<String> tagNames) {
        return tagNames.stream()
            .map(name -> {
                String slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
                return tagRepository.findBySlug(slug)
                    .orElseGet(() -> {
                        Tag newTag = Tag.builder()
                            .name(name)
                            .slug(slug)
                            .build();
                        return tagRepository.save(newTag);
                    });
            })
            .collect(Collectors.toSet());
    }

    /**
     * Generate URL-friendly slug from title.
     * Simple approach - handles duplicates by appending number if needed.
     */
    private String generateSlug(String title) {
        String baseSlug = title.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");

        String slug = baseSlug;
        int counter = 1;

        while (postRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        return slug;
    }

    /**
     * Create excerpt from content (for list views).
     */
    private String createExcerpt(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        // Remove HTML tags (simple approach)
        String text = content.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        if (text.length() <= MAX_EXCERPT_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_EXCERPT_LENGTH - 3) + "...";
    }

    /**
     * Map Post entity to PostResponse.
     */
    private PostResponse mapToResponse(Post post) {
        Set<PostResponse.TagDto> tagDtos = post.getTags().stream()
            .map(tag -> new PostResponse.TagDto(tag.getId(), tag.getName(), tag.getSlug()))
            .collect(Collectors.toSet());

        PostResponse.AuthorInfo authorInfo = new PostResponse.AuthorInfo(
            post.getAuthor().getId(),
            post.getAuthor().getUsername()
        );

        return new PostResponse(
            post.getId(),
            post.getTitle(),
            post.getSlug(),
            post.getContent(),
            post.getStatus(),
            post.getFeaturedImage(),
            post.getCreatedAt(),
            post.getUpdatedAt(),
            authorInfo,
            tagDtos,
            post.getComments().size()
        );
    }

    /**
     * Map Post entity to PostListItem (lighter version for lists).
     */
    private PostListItem mapToListItem(Post post) {
        Set<PostListItem.TagDto> tagDtos = post.getTags().stream()
            .map(tag -> new PostListItem.TagDto(tag.getId(), tag.getName(), tag.getSlug()))
            .collect(Collectors.toSet());

        PostListItem.AuthorInfo authorInfo = new PostListItem.AuthorInfo(
            post.getAuthor().getId(),
            post.getAuthor().getUsername()
        );

        return new PostListItem(
            post.getId(),
            post.getTitle(),
            post.getSlug(),
            createExcerpt(post.getContent()),
            post.getStatus(),
            post.getCreatedAt(),
            post.getUpdatedAt(),
            authorInfo,
            tagDtos
        );
    }
}
