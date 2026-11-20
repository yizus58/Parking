package com.nelumbo.park.infrastructure.config.initialization;

import com.nelumbo.park.infrastructure.config.initialization.DatabaseCreationInitializer.DatabaseUrl;
import com.nelumbo.park.support.TestPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DatabaseCreationInitializerTest {

    private static MockEnvironment initialize(MockEnvironment env) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.setEnvironment(env);
        new DatabaseCreationInitializer().initialize(context);
        return env;
    }

    @Test
    void parse_jdbcUrl() {
        DatabaseUrl url = DatabaseCreationInitializer.parse("jdbc:postgresql://localhost:5332/park?sslmode=disable");

        assertEquals("jdbc:postgresql://localhost:5332/park?sslmode=disable", url.jdbcUrl());
        assertEquals("jdbc:postgresql://localhost:5332/postgres?sslmode=disable", url.adminJdbcUrl());
        assertEquals("park", url.database());
        assertNull(url.user());
        assertNull(url.password());
    }

    @Test
    void parse_providerUrlWithCredentials() {
        DatabaseUrl url = DatabaseCreationInitializer.parse("postgresql://app:p%40ss@db.example.com/park_db");

        assertEquals("jdbc:postgresql://db.example.com/park_db", url.jdbcUrl());
        assertEquals("park_db", url.database());
        assertEquals("app", url.user());
        assertEquals("p@ss", url.password());
        assertEquals("jdbc:postgresql://h:1/d", DatabaseCreationInitializer.parse("postgres://h:1/d").jdbcUrl());
    }

    @Test
    void parse_rejectsNonPostgresUrls() {
        assertThrows(IllegalArgumentException.class, () -> DatabaseCreationInitializer.parse("jdbc:h2:mem:test"));
        assertThrows(IllegalArgumentException.class, () -> DatabaseCreationInitializer.parse("jdbc:postgresql://host-only"));
    }

    @Test
    void jdbcUrl_isKeptAsIs_andCheckCanBeDisabled() {
        MockEnvironment env = initialize(new MockEnvironment()
                .withProperty("SPRING_DATASOURCE_URL", "jdbc:postgresql://127.0.0.1:1/park?sslmode=disable")
                .withProperty("app.database.create-if-missing", "false"));

        assertEquals("jdbc:postgresql://127.0.0.1:1/park?sslmode=disable", env.getProperty("spring.datasource.url"));
    }

    @Test
    void embeddedCredentials_areUsedOnlyWhenNotConfigured() {
        MockEnvironment fromUrl = initialize(new MockEnvironment()
                .withProperty("SPRING_DATASOURCE_URL", "postgresql://app:pw@127.0.0.1:1/park")
                .withProperty("app.database.create-if-missing", "false"));
        assertEquals("app", fromUrl.getProperty("spring.datasource.username"));
        assertEquals("pw", fromUrl.getProperty("spring.datasource.password"));

        MockEnvironment configured = initialize(new MockEnvironment()
                .withProperty("SPRING_DATASOURCE_URL", "postgresql://app:pw@127.0.0.1:1/park")
                .withProperty("SPRING_DATASOURCE_USERNAME", "other")
                .withProperty("spring.datasource.username", "other")
                .withProperty("app.database.create-if-missing", "false"));
        assertEquals("other", configured.getProperty("spring.datasource.username"));
    }

    @Test
    void unreachableServer_isLoggedNotThrown() {
        MockEnvironment env = initialize(new MockEnvironment()
                .withProperty("SPRING_DATASOURCE_URL", "jdbc:postgresql://127.0.0.1:1/park?connectTimeout=1"));

        assertEquals("jdbc:postgresql://127.0.0.1:1/park?connectTimeout=1", env.getProperty("spring.datasource.url"));
    }

    @Test
    void missingDatabase_isCreated() throws Exception {
        assumeTrue(TestPostgres.connection().isPresent(), "needs PostgreSQL");
        TestPostgres.Connection c = TestPostgres.connection().get();
        String name = "park_init_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String url = c.jdbcUrl().replaceFirst("/[^/?]+(\\?|$)", "/" + name + "$1");
        try {
            initialize(new MockEnvironment()
                    .withProperty("SPRING_DATASOURCE_URL", url)
                    .withProperty("SPRING_DATASOURCE_USERNAME", c.username())
                    .withProperty("SPRING_DATASOURCE_PASSWORD", c.password()));

            try (Connection admin = DriverManager.getConnection(c.jdbcUrl(), c.username(), c.password());
                 Statement st = admin.createStatement();
                 ResultSet rs = st.executeQuery("select 1 from pg_database where datname = '" + name + "'")) {
                assertTrue(rs.next(), "database " + name + " was not created");
            }
        } finally {
            try (Connection admin = DriverManager.getConnection(c.jdbcUrl(), c.username(), c.password());
                 Statement st = admin.createStatement()) {
                st.execute("drop database if exists " + name);
            }
        }
    }
}
