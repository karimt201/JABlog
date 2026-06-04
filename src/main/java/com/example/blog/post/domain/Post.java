package com.example.blog.post.domain;

import com.example.blog.tag.domain.Tag;
import com.example.blog.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Post entity - the main content of the blog.
 * Uses slug for SEO-friendly URLs.
 */
@Entity
@Table(name = "posts", uniqueConstraints = @UniqueConstraint(columnNames = "slug"))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PostStatus status = PostStatus.DRAFT;

    @Column(length = 255)
    private String featuredImage;

    // Metadata
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<com.example.blog.comment.domain.Comment> comments = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "post_tags",
        joinColumns = @JoinColumn(name = "post_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    /**
     * Helper method to add a tag to this post.
     * Maintains bidirectional relationship.
     */
    public void addTag(Tag tag) {
        tags.add(tag);
        tag.getPosts().add(this);
    }

    /**
     * Helper method to remove a tag from this post.
     * Maintains bidirectional relationship.
     */
    public void removeTag(Tag tag) {
        tags.remove(tag);
        tag.getPosts().remove(this);
    }

    /**
     * Helper to check if post is published.
     */
    public boolean isPublished() {
        return status == PostStatus.PUBLISHED;
    }

    /**
     * Helper to check if given user is the author.
     */
    public boolean isAuthor(Long userId) {
        return author != null && author.getId().equals(userId);
    }
}
