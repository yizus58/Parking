package com.nelumbo.park.config.initialization;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseCreationInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

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
        } catch (Exception e) {
            System.err.println("Error al verificar/crear la base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean checkDatabaseExists(Connection connection, String databaseName) throws Exception {
        String query = "SELECT 1 FROM pg_database WHERE datname = ?";
        try (var preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, databaseName);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void createDatabase(Connection connection, String databaseName) throws Exception {
        String createDatabaseSQL = "CREATE DATABASE \"" + databaseName + "\"";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(createDatabaseSQL);
        }
    }
}
