package com.example.blog.comment.service;

import com.example.blog.auth.service.AuthService;
import com.example.blog.common.exception.AuthorizationException;
import com.example.blog.common.exception.ResourceNotFoundException;
import com.example.blog.comment.domain.Comment;
import com.example.blog.comment.dto.CommentDtos.*;
import com.example.blog.comment.repository.CommentRepository;
import com.example.blog.post.domain.Post;
import com.example.blog.post.domain.PostStatus;
import com.example.blog.post.repository.PostRepository;
import com.example.blog.user.domain.User;
import com.example.blog.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Comment service handling comment creation and deletion.
 * Comments can only be added to published posts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    /**
     * Create a new comment on a post.
     * Post must be published.
     */
    @Transactional
    public CommentResponse createComment(Long postId, CreateCommentRequest request) {
        Long currentUserId = authService.getCurrentUserId();

        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", postId.toString()));

        // Only allow comments on published posts
        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new AuthorizationException("Comments are only allowed on published posts");
        }

        User author = userRepository.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId.toString()));

        Comment comment = Comment.builder()
            .body(request.getBody())
            .post(post)
            .author(author)
            .build();

        Comment savedComment = commentRepository.save(comment);
        log.info("Comment created on post {} by user {}", postId, author.getUsername());

        return mapToResponse(savedComment);
    }

    /**
     * Delete a comment.
     * Only the comment author can delete their own comments (admins too).
     */
    @Transactional
    public void deleteComment(Long commentId) {
        Long currentUserId = authService.getCurrentUserId();

        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId.toString()));

        if (!comment.isAuthor(currentUserId) && !authService.isAdmin()) {
            throw new AuthorizationException("You can only delete your own comments");
        }

        commentRepository.delete(comment);
        log.info("Comment {} deleted by user {}", commentId, currentUserId);
    }

    /**
     * Get comments for a post (paginated).
     */
    public Page<CommentResponse> getCommentsForPost(Long postId, int page, int size) {
        // Verify post exists and is published
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", postId.toString()));

        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new AuthorizationException("Post is not published");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId, pageable)
            .map(this::mapToResponse);
    }

    /**
     * Get a single comment by ID.
     */
    public CommentResponse getCommentById(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId.toString()));

        return mapToResponse(comment);
    }

    /**
     * Map Comment entity to CommentResponse.
     */
    private CommentResponse mapToResponse(Comment comment) {
        return new CommentResponse(
            comment.getId(),
            comment.getBody(),
            comment.getCreatedAt(),
            new CommentResponse.AuthorInfo(
                comment.getAuthor().getId(),
                comment.getAuthor().getUsername()
            )
        );
    }
}
