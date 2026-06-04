package com.example.blog.post;

import com.example.blog.auth.dto.AuthDtos.AuthResponse;
import com.example.blog.auth.dto.AuthDtos.LoginRequest;
import com.example.blog.auth.dto.AuthDtos.RegisterRequest;
import com.example.blog.post.domain.PostStatus;
import com.example.blog.post.dto.PostDtos.*;
import com.example.blog.post.repository.PostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RedisContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for post endpoints.
 * Tests CRUD operations, authorization, and filtering.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PostIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostRepository postRepository;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test")
        .withStartupTimeout(Duration.ofSeconds(60));

    @Container
    static RedisContainer redis = new RedisContainer("redis:7-alpine")
        .withExposedPorts(6379)
        .withStartupTimeout(Duration.ofSeconds(30));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getFirstMappedPort());
        registry.add("app.jwt.secret", () -> "test-secret-key-for-testing-only-min-256-bits-long-for-encryption-algorithm-to-work");
    }

    private String userToken;
    private Long userId;

    @BeforeEach
    void setUp() throws Exception {
        postRepository.deleteAll();

        // Register a test user
        RegisterRequest registerRequest = new RegisterRequest("testuser", "test@example.com", "password123");
        var result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String response = result.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(response, AuthResponse.class);
        userToken = authResponse.accessToken();
        userId = authResponse.user().id();
    }

    @AfterEach
    void tearDown() {
        postRepository.deleteAll();
    }

    @Test
    void shouldCreatePost() throws Exception {
        CreatePostRequest request = CreatePostRequest.builder()
            .title("My First Post")
            .content("This is the content of my first post")
            .status(PostStatus.PUBLISHED)
            .tagNames(Set.of("java", "spring-boot"))
            .build();

        mockMvc.perform(post("/api/v1/posts")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("My First Post"))
            .andExpect(jsonPath("$.data.slug").exists())
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
            .andExpect(jsonPath("$.data.author.username").value("testuser"))
            .andExpect(jsonPath("$.data.tags", hasSize(2)));
    }

    @Test
    void shouldNotCreatePostWithoutAuth() throws Exception {
        CreatePostRequest request = CreatePostRequest.builder()
            .title("My First Post")
            .content("This is the content of my first post")
            .build();

        mockMvc.perform(post("/api/v1/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldGetPublishedPosts() throws Exception {
        // Create a published post
        CreatePostRequest request = CreatePostRequest.builder()
            .title("Public Post")
            .content("This post is published")
            .status(PostStatus.PUBLISHED)
            .build();

        mockMvc.perform(post("/api/v1/posts")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        // Get published posts (no auth required)
        mockMvc.perform(get("/api/v1/posts/published"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content", hasSize(1)))
            .andExpect(jsonPath("$.data.content[0].title").value("Public Post"));
    }

    @Test
    void shouldGetMyPostsIncludingDrafts() throws Exception {
        // Create a draft post
        CreatePostRequest request = CreatePostRequest.builder()
            .title("Draft Post")
            .content("This is a draft")
            .status(PostStatus.DRAFT)
            .build();

        mockMvc.perform(post("/api/v1/posts")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        // Get my posts
        mockMvc.perform(get("/api/v1/posts/my")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content", hasSize(1)))
            .andExpect(jsonPath("$.data.content[0].title").value("Draft Post"))
            .andExpect(jsonPath("$.data.content[0].status").value("DRAFT"));
    }

    @Test
    void shouldUpdatePost() throws Exception {
        // Create a post
        CreatePostRequest createRequest = CreatePostRequest.builder()
            .title("Original Title")
            .content("Original content")
            .build();

        var createResult = mockMvc.perform(post("/api/v1/posts")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String response = createResult.getResponse().getContentAsString();
        PostResponse postResponse = objectMapper.readValue(response, ApiResponse.class).getData();

        // Update the post
        UpdatePostRequest updateRequest = UpdatePostRequest.builder()
            .title("Updated Title")
            .content("Updated content")
            .status(PostStatus.PUBLISHED)
            .build();

        mockMvc.perform(put("/api/v1/posts/" + postResponse.id())
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("Updated Title"))
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    void shouldNotUpdateAnotherUsersPost() throws Exception {
        // Create a post as first user
        CreatePostRequest request = CreatePostRequest.builder()
            .title("My Post")
            .content("My content")
            .build();

        var createResult = mockMvc.perform(post("/api/v1/posts")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();

        String response = createResult.getResponse().getContentAsString();
        ApiResponse<PostResponse> apiResponse = objectMapper.readValue(response,
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, PostResponse.class));
        Long postId = apiResponse.getData().id();

        // Try to update as a different user
        RegisterRequest registerRequest2 = new RegisterRequest("otheruser", "other@example.com", "password123");
        var registerResult = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest2)))
            .andExpect(status().isCreated())
            .andReturn();

        String authResponse = registerResult.getResponse().getContentAsString();
        AuthResponse otherAuth = objectMapper.readValue(authResponse, AuthResponse.class);

        // Try to update with other user's token
        UpdatePostRequest updateRequest = UpdatePostRequest.builder()
            .title("Hacked Title")
            .content("Hacked content")
            .build();

        mockMvc.perform(put("/api/v1/posts/" + postId)
                .header("Authorization", "Bearer " + otherAuth.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldDeletePost() throws Exception {
        // Create a post
        CreatePostRequest request = CreatePostRequest.builder()
            .title("To Delete")
            .content("This will be deleted")
            .build();

        var createResult = mockMvc.perform(post("/api/v1/posts")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();

        String response = createResult.getResponse().getContentAsString();
        ApiResponse<PostResponse> apiResponse = objectMapper.readValue(response,
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, PostResponse.class));
        Long postId = apiResponse.getData().id();

        // Delete the post
        mockMvc.perform(delete("/api/v1/posts/" + postId)
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Verify it's deleted
        mockMvc.perform(get("/api/v1/posts/" + postId)
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldFilterPostsByTag() throws Exception {
        // Create posts with tags
        CreatePostRequest request1 = CreatePostRequest.builder()
            .title("Java Post")
            .content("Java content")
            .status(PostStatus.PUBLISHED)
            .tagNames(Set.of("java"))
            .build();

        CreatePostRequest request2 = CreatePostRequest.builder()
            .title("Python Post")
            .content("Python content")
            .status(PostStatus.PUBLISHED)
            .tagNames(Set.of("python"))
            .build();

        mockMvc.perform(post("/api/v1/posts")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)));

        mockMvc.perform(post("/api/v1/posts")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)));

        // Filter by java tag
        mockMvc.perform(get("/api/v1/posts/tag/java"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", hasSize(1)))
            .andExpect(jsonPath("$.data.content[0].title").value("Java Post"));
    }

    @Test
    void shouldPublishPost() throws Exception {
        // Create a draft post
        CreatePostRequest request = CreatePostRequest.builder()
            .title("Draft")
            .content("Draft content")
            .status(PostStatus.DRAFT)
            .build();

        var createResult = mockMvc.perform(post("/api/v1/posts")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();

        String response = createResult.getResponse().getContentAsString();
        ApiResponse<PostResponse> apiResponse = objectMapper.readValue(response,
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, PostResponse.class));
        Long postId = apiResponse.getData().id();

        // Publish it
        PublishRequest publishRequest = new PublishRequest(PostStatus.PUBLISHED);
        mockMvc.perform(patch("/api/v1/posts/" + postId + "/status")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(publishRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        // Verify it's now in published posts
        mockMvc.perform(get("/api/v1/posts/published"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", hasSize(1)));
    }

    // Helper record for JSON parsing
    record ApiResponse<T>(Boolean success, T data) {}
}
