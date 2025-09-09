package com.nelumbo.park.config.initialization;

import com.nelumbo.park.exception.exceptions.DatabaseInitializationException;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseCreationInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(DatabaseCreationInitializer.class);

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();

        String databaseUrl = env.getProperty("SPRING_DATASOURCE_URL", "jdbc:postgresql://localhost:5332/park");
        String databaseUsername = env.getProperty("SPRING_DATASOURCE_USERNAME", "postgres");
        String databasePassword = env.getProperty("SPRING_DATASOURCE_PASSWORD", "root");
        String driverClassName = env.getProperty("SPRING_DATASOURCE_DRIVER_CLASS_NAME", "org.postgresql.Driver");

        String[] urlParts = databaseUrl.split("/");
        String databaseName = urlParts[urlParts.length - 1];
        String baseUrl = databaseUrl.substring(0, databaseUrl.lastIndexOf("/")) + "/postgres";

        try {
            Class.forName(driverClassName);

            try (Connection connection = DriverManager.getConnection(baseUrl, databaseUsername, databasePassword)) {

                boolean databaseExists = checkDatabaseExists(connection, databaseName);

                if (!databaseExists) {
                    createDatabase(connection, databaseName);
                }
            }
        } catch (ClassNotFoundException e) {
            logger.error("Error al cargar el driver de la base de datos: {}", e.getMessage());
        } catch (SQLException e) {
            logger.error("Error de SQL al verificar/crear la base de datos: {}", e.getMessage());
        } catch (DatabaseInitializationException e) {
            logger.error("Error al verificar/crear la base de datos: {}", e.getMessage());
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
