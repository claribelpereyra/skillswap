package com.uade.skillswap.app;

import com.uade.skillswap.infrastructure.persistence.PersistenceConfiguration;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

/** Crea una base desechable propia por test. Nunca usa ni limpia la base de la aplicación. */
public final class TestDatabase implements AutoCloseable {
    private final String server;
    private final String user;
    private final String password;
    private final String schema = "skillswap_it_" + UUID.randomUUID().toString().replace("-", "");

    public TestDatabase() {
        String configured = System.getenv("SKILLSWAP_MYSQL_TEST_URL");
        server = configured == null || configured.isBlank() ? null : configured;
        user = System.getenv().getOrDefault("SKILLSWAP_MYSQL_TEST_USER", "root");
        password = System.getenv().getOrDefault("SKILLSWAP_MYSQL_TEST_PASSWORD", "root");
        if (server != null) {
            if (!server.startsWith("jdbc:mysql://") || !server.endsWith("/"))
                throw new IllegalArgumentException("SKILLSWAP_MYSQL_TEST_URL must be a MySQL server URL ending in /");
            execute("CREATE DATABASE `" + schema + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_cs");
        }
    }

    public PersistenceConfiguration configuration() {
        if (server == null) return new PersistenceConfiguration("jdbc:h2:mem:" + schema
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        if (!server.startsWith("jdbc:mysql://") || !server.endsWith("/"))
            throw new IllegalArgumentException("SKILLSWAP_MYSQL_TEST_URL must be a MySQL server URL ending in /");
        return new PersistenceConfiguration(server + schema + "?connectionTimeZone=UTC", user, password);
    }

    private void execute(String sql) {
        try (var connection = DriverManager.getConnection(server, user, password);
             var statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException error) {
            throw new IllegalStateException("Test database operation failed", error);
        }
    }

    @Override public void close() {
        if (server != null && schema.matches("skillswap_it_[a-f0-9]{32}")) execute("DROP DATABASE `" + schema + "`");
    }
}
