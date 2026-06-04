# JA Blog

A practical Spring Boot blog platform with JWT authentication, built with real-world patterns in mind. Not over-engineered, not boilerplate - just solid, working code.

## Tech Stack

- **Java 21** (LTS) with Virtual Threads
- **Spring Boot 3.4.2**
- **Spring Security 6** (JWT-based stateless auth)
- **PostgreSQL** (primary database)
- **Redis** (caching + refresh token storage)
- **Flyway** (database migrations)
- **Testcontainers** (integration tests with real containers)
- **Docker Compose** (local development)

## Features

- **Authentication**: Register, login, logout, token refresh with JWT + Redis
- **Posts**: CRUD operations, publish/unpublish, tag management
- **Comments**: Create and delete (own comments only)
- **Pagination**: All list endpoints support pagination
- **Filtering**: Filter posts by status, author, tags
- **Search**: Full-text search in posts
- **Caching**: Redis caching for frequently accessed data
- **API Docs**: Swagger UI at `/swagger-ui.html`

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker (for PostgreSQL + Redis)

### Running with Docker Compose (Recommended)

```bash
# Start the infrastructure services
docker-compose up postgres redis

# Run the application
mvn spring-boot:run

# Or build and run everything together
docker-compose up
```

### Manual Setup

```bash
# Create database
createdb jablog

# Set environment variables
export DATABASE_URL=jdbc:postgresql://localhost:5432/jablog
export DATABASE_USER=your_user
export DATABASE_PASSWORD=your_password
export REDIS_HOST=localhost
export REDIS_PORT=6379
export JWT_SECRET=your-secret-key-at-least-256-bits

# Run the app
mvn spring-boot:run
```

The app will be available at `http://localhost:8080`

## API Documentation

Once running, visit `http://localhost:8080/swagger-ui.html` for interactive API documentation.

## Key Endpoints

### Authentication
- `POST /api/v1/auth/register` - Register new user
- `POST /api/v1/auth/login` - Login
- `POST /api/v1/auth/logout` - Logout
- `POST /api/v1/auth/refresh` - Refresh access token
- `GET /api/v1/auth/me` - Get current user info

### Posts
- `GET /api/v1/posts/published` - Get published posts (public)
- `GET /api/v1/posts/my` - Get current user's posts (incl. drafts)
- `GET /api/v1/posts/{id}` - Get post by ID
- `POST /api/v1/posts` - Create post
- `PUT /api/v1/posts/{id}` - Update post
- `DELETE /api/v1/posts/{id}` - Delete post
- `PATCH /api/v1/posts/{id}/status` - Publish/unpublish post
- `GET /api/v1/posts/tag/{slug}` - Get posts by tag
- `GET /api/v1/posts/search?keyword=` - Search posts

### Comments
- `GET /api/v1/posts/{postId}/comments` - Get comments for post
- `POST /api/v1/posts/{postId}/comments` - Create comment
- `DELETE /api/v1/comments/{id}` - Delete own comment

### Tags
- `GET /api/v1/tags` - Get all tags
- `POST /api/v1/tags` - Create tag (auth required)

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AuthIntegrationTest

# Run with coverage
mvn clean test jacoco:report
```

Tests use Testcontainers to spin up real PostgreSQL and Redis containers, so they run slower than unit tests but verify real integration.

## Project Structure

```
src/main/java/com/example/blog/
├── auth/              # JWT, Security, Auth controller
├── post/              # Post CRUD, filtering, pagination
├── comment/           # Comment management
├── tag/               # Tag management
├── user/              # User domain
├── common/            # Shared config, exceptions, responses
└── BlogApplication.java
```

## Configuration

### Profiles

- `dev` (default): SQL logging, debug output
- `prod`: Production-ready settings

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DATABASE_URL` | JDBC URL | jdbc:postgresql://localhost:5432/jablog |
| `DATABASE_USER` | DB username | jablog_user |
| `DATABASE_PASSWORD` | DB password | jablog_pass |
| `REDIS_HOST` | Redis host | localhost |
| `REDIS_PORT` | Redis port | 6379 |
| `JWT_SECRET` | JWT signing key | **CHANGE IN PROD** |
| `SERVER_PORT` | HTTP port | 8080 |

## Known Issues / TODOs

- The popular tags endpoint orders by createdAt instead of post count (see TagService:98)
- Could add nested comments (currently only flat)
- Email verification for registration
- Password reset flow
- Rate limiting on auth endpoints
- Actuator health endpoint (added in Dockerfile but not configured)

## Production Checklist

Before deploying to production:

1. **Change JWT_SECRET** - Use a strong, randomly generated key
2. **Configure CORS** - Update allowed origins for your frontend
3. **Review security headers** - Add CSP, X-Frame-Options, etc.
4. **Enable HTTPS** - Configure SSL/TLS
5. **Set up monitoring** - Add metrics (Micrometer + Prometheus)
6. **Review database pool** - Adjust Hikari settings based on load
7. **Configure backups** - Set up automated DB backups
8. **Review cache settings** - Adjust Redis TTL for your needs

## License

MIT License - feel free to use this for learning or projects.
