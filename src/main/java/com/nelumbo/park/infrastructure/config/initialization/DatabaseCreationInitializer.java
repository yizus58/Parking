package com.nelumbo.park.infrastructure.config.initialization;

import com.nelumbo.park.infrastructure.exception.DatabaseInitializationException;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalises {@code SPRING_DATASOURCE_URL} to a JDBC URL and creates the
 * database if it does not exist yet, before the context starts (the tables
 * are then created by the Flyway migrations).
 *
 * <p>Accepted URL forms: {@code jdbc:postgresql://host[:port]/db[?params]} and
 * the provider form {@code postgres[ql]://[user[:password]@]host[:port]/db[?params]}.
 * Credentials embedded in the URL are used only when
 * {@code SPRING_DATASOURCE_USERNAME} / {@code SPRING_DATASOURCE_PASSWORD} are not set.
 *
 * <p>Disable the existence check with {@code app.database.create-if-missing=false}.
 */
public class DatabaseCreationInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(DatabaseCreationInitializer.class);

    private static final Pattern URL = Pattern.compile(
            "^(?:jdbc:)?postgres(?:ql)?://(?:(?<user>[^:@/]*)(?::(?<password>[^@/]*))?@)?(?<host>[^/?]+)/(?<db>[^?]+)(?<params>\\?.*)?$");

    record DatabaseUrl(String jdbcUrl, String adminJdbcUrl, String database, String user, String password) {
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment env = applicationContext.getEnvironment();
        String originalUrl = env.getProperty("SPRING_DATASOURCE_URL");
        if (originalUrl == null || originalUrl.isBlank()) {
            logger.warn("SPRING_DATASOURCE_URL is not set. Skipping database initialization check.");
            return;
        }

        DatabaseUrl url;
        try {
            url = parse(originalUrl);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage());
            return;
        }

        Map<String, Object> overrides = new HashMap<>();
        overrides.put("spring.datasource.url", url.jdbcUrl());
        String username = env.getProperty("SPRING_DATASOURCE_USERNAME");
        String password = env.getProperty("SPRING_DATASOURCE_PASSWORD");
        if ((username == null || username.isBlank()) && url.user() != null) {
            username = url.user();
            overrides.put("spring.datasource.username", username);
        }
        if ((password == null || password.isBlank()) && url.password() != null) {
            password = url.password();
            overrides.put("spring.datasource.password", password);
        }
        env.getPropertySources().addFirst(new MapPropertySource("normalized-datasource", overrides));
        logger.info("Using spring.datasource.url={}", url.jdbcUrl());

        if (!env.getProperty("app.database.create-if-missing", Boolean.class, true)) {
            return;
        }
        try (Connection connection = DriverManager.getConnection(url.adminJdbcUrl(),
                username == null ? "" : username, password == null ? "" : password)) {
            if (!databaseExists(connection, url.database())) {
                createDatabase(connection, url.database());
            }
        } catch (SQLException | DatabaseInitializationException e) {
            logger.error("Could not check or create database '{}': {}", url.database(), e.getMessage());
        }
    }

    /** Package-private for tests. */
    static DatabaseUrl parse(String url) {
        Matcher m = URL.matcher(url.trim());
        if (!m.matches()) {
            throw new IllegalArgumentException("SPRING_DATASOURCE_URL is not a PostgreSQL URL (expected "
                    + "jdbc:postgresql://host:port/db or postgresql://user:password@host:port/db)");
        }
        String host = m.group("host");
        String db = m.group("db");
        String params = m.group("params") == null ? "" : m.group("params");
        return new DatabaseUrl(
                "jdbc:postgresql://" + host + "/" + db + params,
                "jdbc:postgresql://" + host + "/postgres" + params,
                decode(db),
                decode(m.group("user")),
                decode(m.group("password")));
    }

    private static String decode(String value) {
        return value == null ? null : URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private boolean databaseExists(Connection connection, String databaseName) throws DatabaseInitializationException {
        try (var ps = connection.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
            ps.setString(1, databaseName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseInitializationException("Error al verificar la existencia de la base de datos: " + databaseName, e);
        }
    }

    private void createDatabase(Connection connection, String databaseName) throws DatabaseInitializationException {
        String quoted;
        try (var ps = connection.prepareStatement("SELECT pg_catalog.quote_ident(?)")) {
            ps.setString(1, databaseName);
            try (var rs = ps.executeQuery()) {
                rs.next();
                quoted = rs.getString(1);
            }
        } catch (SQLException e) {
            throw new DatabaseInitializationException("Error al citar el nombre de la base de datos: " + databaseName, e);
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE " + quoted);
            logger.info("Base de datos '{}' creada exitosamente", databaseName);
        } catch (SQLException e) {
            throw new DatabaseInitializationException("Error al crear la base de datos: " + databaseName, e);
        }
    }
}
