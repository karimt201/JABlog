package com.example.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main entry point for JA Blog application.
 *
 * Virtual threads are enabled via JVM args in Docker/run scripts,
 * not here in code (keeps it clean).
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
public class BlogApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogApplication.class, args);
    }

    // TODO: Add graceful shutdown hook for production
}
