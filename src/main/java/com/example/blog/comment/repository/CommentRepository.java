package com.example.blog.comment.repository;

import com.example.blog.comment.domain.Comment;
import com.example.blog.post.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Comment repository.
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPostIdOrderByCreatedAtAsc(Long postId, Pageable pageable);

    List<Comment> findByPostId(Long postId);

    boolean existsByPostIdAndAuthorId(Long postId, Long authorId);

    void deleteByPostIdAndAuthorId(Long postId, Long authorId);
}
