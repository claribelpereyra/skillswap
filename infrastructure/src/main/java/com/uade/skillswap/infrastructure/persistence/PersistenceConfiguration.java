package com.uade.skillswap.infrastructure.persistence;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.flywaydb.core.Flyway;

import java.util.Map;

public final class PersistenceConfiguration {

    private static final String PERSISTENCE_UNIT = "skillswapPU";

    private final String url;
    private final String user;
    private final String password;

    public PersistenceConfiguration(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public static PersistenceConfiguration fromEnvironment() {
        return new PersistenceConfiguration(
                env("SKILLSWAP_DB_URL", "jdbc:mysql://localhost:3306/skillswap"),
                env("SKILLSWAP_DB_USER", "skillswap"),
                env("SKILLSWAP_DB_PASSWORD", "skillswap"));
    }

    public void migrate() {
        Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    public EntityManagerFactory createEntityManagerFactory() {
        Map<String, Object> properties = Map.of(
                "jakarta.persistence.jdbc.url", url,
                "jakarta.persistence.jdbc.user", user,
                "jakarta.persistence.jdbc.password", password);
        return Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, properties);
    }

    private static String env(String variable, String defaultValue) {
        String value = System.getenv(variable);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
