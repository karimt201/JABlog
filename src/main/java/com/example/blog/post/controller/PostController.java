package com.example.blog.post.controller;

import com.example.blog.common.exception.ApiResponse;
import com.example.blog.post.domain.PostStatus;
import com.example.blog.post.dto.PostDtos.*;
import com.example.blog.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "Blog post management endpoints")
public class PostController {

    private final PostService postService;

    @PostMapping
    @Operation(summary = "Create a new post")
    public ResponseEntity<ApiResponse<PostResponse>> createPost(@Valid @RequestBody CreatePostRequest request) {
        PostResponse response = postService.createPost(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.created(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get post by ID")
    public ResponseEntity<ApiResponse<PostResponse>> getPostById(@PathVariable Long id) {
        PostResponse response = postService.getPostById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get post by slug (public view)")
    public ResponseEntity<ApiResponse<PostResponse>> getPostBySlug(@PathVariable String slug) {
        PostResponse response = postService.getPostBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/published")
    @Operation(summary = "Get all published posts (public)")
    public ResponseEntity<ApiResponse<Page<PostListItem>>> getPublishedPosts(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        int actualSize = Math.min(size, 100);
        Page<PostListItem> response = postService.getPublishedPosts(page, actualSize);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my")
    @Operation(summary = "Get current user's posts (including drafts)")
    public ResponseEntity<ApiResponse<Page<PostListItem>>> getMyPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int actualSize = Math.min(size, 100);
        Page<PostListItem> response = postService.getMyPosts(page, actualSize);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/author/{authorId}")
    @Operation(summary = "Get posts by author (published only)")
    public ResponseEntity<ApiResponse<Page<PostListItem>>> getPostsByAuthor(
            @PathVariable Long authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int actualSize = Math.min(size, 100);
        Page<PostListItem> response = postService.getPostsByAuthor(authorId, page, actualSize);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/tag/{tagSlug}")
    @Operation(summary = "Get posts by tag (published only)")
    public ResponseEntity<ApiResponse<Page<PostListItem>>> getPostsByTag(
            @PathVariable String tagSlug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int actualSize = Math.min(size, 100);
        Page<PostListItem> response = postService.getPostsByTag(tagSlug, page, actualSize);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @Operation(summary = "Search posts by keyword (published only)")
    public ResponseEntity<ApiResponse<Page<PostListItem>>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int actualSize = Math.min(size, 100);
        Page<PostListItem> response = postService.searchPosts(keyword, page, actualSize);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/filter")
    @Operation(summary = "Filter posts by status, author, and/or tag")
    public ResponseEntity<ApiResponse<Page<PostListItem>>> filterPosts(
            @RequestParam(required = false) PostStatus status,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String tagSlug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int actualSize = Math.min(size, 100);
        Page<PostListItem> response = postService.filterPosts(status, authorId, tagSlug, page, actualSize);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a post")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request) {
        PostResponse response = postService.updatePost(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a post")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.ok(ApiResponse.success("Post deleted successfully", null));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Publish or unpublish a post")
    public ResponseEntity<ApiResponse<PostResponse>> setPostStatus(
            @PathVariable Long id,
            @RequestBody PublishRequest request) {
        PostResponse response = postService.setPostStatus(id, request.status());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
