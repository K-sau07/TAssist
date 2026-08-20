package com.tassist.infrastructure.persistence;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Boots a real pgvector Postgres via Testcontainers, runs Flyway V1-V8 against it,
 * and wires the full Spring context so persistence adapters can be exercised end-to-end.
 */
@SpringBootTest
@Testcontainers
public abstract class AbstractPgvectorContainerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres")
    );

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        // Redis is not needed for persistence slice tests; disable its auto-config health if present.
        r.add("spring.data.redis.repositories.enabled", () -> "false");
    }
}
