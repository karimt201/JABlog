package com.example.blog.tag.repository;

import com.example.blog.tag.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Tag repository.
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT t FROM Tag t LEFT JOIN FETCH t.posts WHERE t.slug = :slug")
    Optional<Tag> findBySlugWithPosts(@Param("slug") String slug);

    boolean existsByName(String name);
}
