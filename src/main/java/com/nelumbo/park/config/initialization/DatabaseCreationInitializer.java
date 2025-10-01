package com.nelumbo.park.config.initialization;

import com.nelumbo.park.exception.exceptions.DatabaseInitializationException;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseCreationInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(DatabaseCreationInitializer.class);

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment env = applicationContext.getEnvironment();
        
        String originalUrl = env.getProperty("SPRING_DATASOURCE_URL");
        String dbUsername = env.getProperty("SPRING_DATASOURCE_USERNAME", "postgres");
        String dbPassword = env.getProperty("SPRING_DATASOURCE_PASSWORD", "root");
        String driverClassName = env.getProperty("SPRING_DATASOURCE_DRIVER_CLASS_NAME", "org.postgresql.Driver");

        if (originalUrl == null) {
            logger.warn("SPRING_DATASOURCE_URL is not set. Skipping database initialization check.");
            return;
        }

        String hostPart;
        String dbNamePart;
        
        String urlToParse = originalUrl;
        if (urlToParse.startsWith("postgresql://")) {
            urlToParse = urlToParse.substring("postgresql://".length());
        }

        int atIndex = urlToParse.indexOf('@');
        if (atIndex != -1) {
            urlToParse = urlToParse.substring(atIndex + 1);
        }

        int slashIndex = urlToParse.lastIndexOf('/');
        if (slashIndex != -1) {
            hostPart = urlToParse.substring(0, slashIndex);
            dbNamePart = urlToParse.substring(slashIndex + 1);
        } else {
            logger.error("Could not parse host and database name from SPRING_DATASOURCE_URL: {}", originalUrl);
            return;
        }

        String correctedAppUrl = "jdbc:postgresql://" + hostPart + "/" + dbNamePart;
        MutablePropertySources propertySources = env.getPropertySources();
        Properties props = new Properties();
        props.put("spring.datasource.url", correctedAppUrl);
        propertySources.addFirst(new PropertiesPropertySource("corrected-db-url", props));
        logger.info("Overriding spring.datasource.url with: {}", correctedAppUrl);

        String baseUrlForCheck = "jdbc:postgresql://" + hostPart + "/postgres";

        try {
            Class.forName(driverClassName);
            try (Connection connection = DriverManager.getConnection(baseUrlForCheck, dbUsername, dbPassword)) {
                boolean databaseExists = checkDatabaseExists(connection, dbNamePart);
                if (!databaseExists) {
                    createDatabase(connection, dbNamePart);
                }
            }
        } catch (ClassNotFoundException e) {
            logger.error("Error loading database driver: {}", e.getMessage());
        } catch (SQLException e) {
            logger.error("SQL error during database check/creation: {}", e.getMessage());
        } catch (DatabaseInitializationException e) {
            logger.error("Error during database check/creation: {}", e.getMessage());
        }
    }

    private boolean checkDatabaseExists(Connection connection, String databaseName) throws DatabaseInitializationException {
        String query = "SELECT 1 FROM pg_database WHERE datname = ?";
        try (var preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, databaseName);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new DatabaseInitializationException("Error al verificar la existencia de la base de datos: " + databaseName, e);
        }
    }

    private void createDatabase(Connection connection, String databaseName) throws DatabaseInitializationException {
        String createDatabaseSQL = "CREATE DATABASE \"" + databaseName + "\"";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(createDatabaseSQL);
            logger.info("Base de datos '{}' creada exitosamente", databaseName);
        } catch (SQLException e) {
            throw new DatabaseInitializationException("Error al crear la base de datos: " + databaseName, e);
        }
    }
}
