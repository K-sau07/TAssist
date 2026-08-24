package com.tassist.infrastructure.persistence;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Boots a real pgvector Postgres via Testcontainers, runs Flyway against it, and wires the full
 * Spring context so persistence adapters can be exercised end-to-end.
 *
 * <p><b>Singleton container pattern:</b> the container is started once in a static initializer and
 * intentionally never stopped (the JVM/Ryuk reaps it at exit). This is deliberate — the previous
 * {@code @Container}/{@code @Testcontainers} setup restarted the container per test class, which under
 * limited Docker memory caused start-timeouts and "relation does not exist" errors when more than one
 * class extended this base. One shared warm container is faster and reliable.
 */
@SpringBootTest
public abstract class AbstractPgvectorContainerTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres")
    );

    static {
        POSTGRES.start(); // start once for the whole JVM; not stopped per class
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        r.add("spring.data.redis.repositories.enabled", () -> "false");
    }
}
