package com.example.blog.post.repository;

import com.example.blog.post.domain.Post;
import com.example.blog.post.domain.PostStatus;
import com.example.blog.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Post repository with queries for listing and filtering posts.
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // Find published posts (public view)
    @Query("SELECT p FROM Post p WHERE p.status = :status")
    Page<Post> findByStatus(@Param("status") PostStatus status, Pageable pageable);

    // Find posts by author
    Page<Post> findByAuthorId(Long authorId, Pageable pageable);

    // Find posts by author and status
    Page<Post> findByAuthorIdAndStatus(Long authorId, PostStatus status, Pageable pageable);

    // Find posts by tag
    @Query("SELECT p FROM Post p JOIN p.tags t WHERE t.slug = :tagSlug AND p.status = :status")
    Page<Post> findByTagSlugAndStatus(@Param("tagSlug") String tagSlug, @Param("status") PostStatus status, Pageable pageable);

    // Search posts by title or content
    @Query("SELECT p FROM Post p WHERE p.status = :status AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Post> searchByKeyword(@Param("keyword") String keyword, @Param("status") PostStatus status, Pageable pageable);

    // Find recent published posts (for homepage)
    @Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' ORDER BY p.createdAt DESC")
    Page<Post> findLatestPublished(Pageable pageable);

    // Check if slug exists
    boolean existsBySlug(String slug);

    // FIXME: refactor this - should probably use a specification or querydsl for complex filtering
    @Query("SELECT p FROM Post p WHERE " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:authorId IS NULL OR p.author.id = :authorId) AND " +
           "(:tagSlug IS NULL OR EXISTS (SELECT 1 FROM p.tags t WHERE t.slug = :tagSlug))")
    Page<Post> filterPosts(@Param("status") PostStatus status,
                          @Param("authorId") Long authorId,
                          @Param("tagSlug") String tagSlug,
                          Pageable pageable);

    // Fetch post with author eagerly
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.author LEFT JOIN FETCH p.tags WHERE p.id = :id")
    Optional<Post> findByIdWithAuthorAndTags(@Param("id") Long id);

    // Fetch post with comments count
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.author LEFT JOIN p.comments c WHERE p.id = :id")
    Optional<Post> findByIdWithAuthor(@Param("id") Long id);
}
