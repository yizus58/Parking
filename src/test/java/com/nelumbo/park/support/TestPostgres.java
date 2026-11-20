package com.nelumbo.park.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Optional;

/**
 * Disposable PostgreSQL for the tests that need the real database (migrations,
 * partial unique index, row locks).
 * <ol>
 *   <li>If {@code TEST_POSTGRES_URL} (a JDBC URL) is set, as an env var or system property, that
 *       database is used, with {@code TEST_POSTGRES_USERNAME} / {@code TEST_POSTGRES_PASSWORD}.
 *       Point it at a throwaway database: the tests run the migrations and delete all rows.</li>
 *   <li>Otherwise, if Docker is available, a Testcontainers {@code postgres:16-alpine} is started once.</li>
 *   <li>Otherwise the PostgreSQL tests are skipped (reported as skipped, not as errors).</li>
 * </ol>
 */
public final class TestPostgres {

    public record Connection(String jdbcUrl, String username, String password, String source) {
    }

    private static Optional<Connection> connection;

    private TestPostgres() {
    }

    public static synchronized Optional<Connection> connection() {
        if (connection == null) {
            connection = resolve();
        }
        return connection;
    }

    /**
     * Call from a {@code @DynamicPropertySource} method: real PostgreSQL, schema
     * created by the Flyway migrations and only validated by Hibernate, as in production.
     */
    public static void registerProperties(DynamicPropertyRegistry registry) {
        connection().ifPresent(c -> {
            registry.add("spring.datasource.url", c::jdbcUrl);
            registry.add("spring.datasource.username", c::username);
            registry.add("spring.datasource.password", c::password);
            registry.add("spring.datasource.driverClassName", () -> "org.postgresql.Driver");
            registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
            registry.add("spring.flyway.enabled", () -> "true");
            registry.add("spring.flyway.locations", () -> "classpath:db/migration");
            registry.add("spring.flyway.baseline-on-migrate", () -> "true");
            registry.add("spring.flyway.baseline-version", () -> "1");
        });
    }

    private static Optional<Connection> resolve() {
        String url = setting("TEST_POSTGRES_URL");
        if (url != null && !url.isBlank()) {
            return Optional.of(new Connection(url, orDefault(setting("TEST_POSTGRES_USERNAME"), "postgres"),
                    orDefault(setting("TEST_POSTGRES_PASSWORD"), ""), "TEST_POSTGRES_URL"));
        }
        if (!dockerAvailable()) {
            return Optional.empty();
        }
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:16-alpine");
        container.start();
        return Optional.of(new Connection(container.getJdbcUrl(), container.getUsername(), container.getPassword(),
                "testcontainers"));
    }

    private static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable e) {
            return false;
        }
    }

    private static String setting(String key) {
        String value = System.getProperty(key);
        return value != null ? value : System.getenv(key);
    }

    private static String orDefault(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
