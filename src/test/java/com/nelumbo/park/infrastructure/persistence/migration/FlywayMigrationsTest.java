package com.nelumbo.park.infrastructure.persistence.migration;

import com.nelumbo.park.support.RequiresPostgres;
import com.nelumbo.park.support.TestPostgres;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The upgrade path of a database created before Flyway (by ddl-auto=update),
 * each scenario in its own throwaway schema.
 */
@RequiresPostgres
class FlywayMigrationsTest {

    private static final String SCHEMA = "flyway_upgrade_test";

    @AfterEach
    void dropSchema() throws SQLException {
        exec("drop schema if exists " + SCHEMA + " cascade");
    }

    @Test
    void emptyDatabase_getsTheWholeSchema() throws SQLException {
        MigrateResult result = flyway().migrate();

        assertEquals(3, result.migrationsExecuted);
        assertEquals(1, count("select count(*) from pg_indexes where schemaname = '" + SCHEMA
                + "' and indexname = 'ux_vehicles_plate_inside'"));
    }

    @Test
    void existingDatabase_isBaselinedAndKeepsItsData() throws Exception {
        givenLegacySchemaWithOneParking();
        exec("insert into " + SCHEMA + ".vehicles values (1000, now(), null, 'v1', 'u1', 'p1', 'Mazda', ' abc123', 'IN')");
        exec("insert into " + SCHEMA + ".vehicles values (1000, now(), now(), 'v2', 'u1', 'p1', 'Mazda', 'abc123', 'OUT')");

        MigrateResult result = flyway().migrate();

        assertEquals(2, result.migrationsExecuted, "V1 is skipped by the baseline, V2 and V3 run");
        assertEquals("1", firstHistoryVersion());
        assertEquals(2, count("select count(*) from " + SCHEMA + ".vehicles"));
        assertEquals(1, count("select count(*) from " + SCHEMA + ".vehicles where id = 'v1' and plate_number = 'ABC123'"),
                "plates still inside are normalised so the unique index covers them");
        assertEquals(1, count("select count(*) from " + SCHEMA + ".vehicles where id = 'v2' and plate_number = 'abc123'"),
                "history is left untouched");
    }

    @Test
    void existingDatabaseWithDuplicateActivePlates_failsWithoutChanges() throws Exception {
        givenLegacySchemaWithOneParking();
        exec("insert into " + SCHEMA + ".vehicles values (1000, now(), null, 'v1', 'u1', 'p1', 'Mazda', 'DUP111', 'IN')");
        exec("insert into " + SCHEMA + ".vehicles values (1000, now(), null, 'v2', 'u1', 'p1', 'Mazda', 'DUP111', 'IN')");

        assertThrows(FlywayException.class, () -> flyway().migrate());

        assertEquals(0, count("select count(*) from pg_indexes where schemaname = '" + SCHEMA
                + "' and indexname = 'ux_vehicles_plate_inside'"));
        assertFalse(count("select count(*) from " + SCHEMA + ".flyway_schema_history where version = '2' and success") > 0);
    }

    private void givenLegacySchemaWithOneParking() throws SQLException, IOException {
        exec("create schema " + SCHEMA);
        exec("set search_path to " + SCHEMA + ";" + resource("db/migration/V1__initial_schema.sql"));
        exec("insert into " + SCHEMA + ".users_table values ('s@example.com', 'u1', 'hash', 'SOCIO', 'socio')");
        exec("insert into " + SCHEMA + ".parking_lot values (10, 1000, 'Calle 1', 'p1', 'u1', 'Centro')");
    }

    private Flyway flyway() {
        TestPostgres.Connection c = TestPostgres.connection().orElseThrow();
        return Flyway.configure()
                .dataSource(c.jdbcUrl(), c.username(), c.password())
                .schemas(SCHEMA)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load();
    }

    private String firstHistoryVersion() throws SQLException {
        try (Connection connection = open(); Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("select version from " + SCHEMA
                     + ".flyway_schema_history order by installed_rank limit 1")) {
            assertTrue(rs.next());
            return rs.getString(1);
        }
    }

    private int count(String sql) throws SQLException {
        try (Connection connection = open(); Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private void exec(String sql) throws SQLException {
        try (Connection connection = open(); Statement st = connection.createStatement()) {
            st.execute(sql);
        }
    }

    private static Connection open() throws SQLException {
        TestPostgres.Connection c = TestPostgres.connection().orElseThrow();
        return DriverManager.getConnection(c.jdbcUrl(), c.username(), c.password());
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = FlywayMigrationsTest.class.getClassLoader().getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
