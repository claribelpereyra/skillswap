package com.uade.skillswap.infraestructura.persistencia;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.flywaydb.core.Flyway;

import java.util.Map;

/**
 * Corre las migraciones de Flyway y crea el EntityManagerFactory.
 * La conexión se toma de variables de entorno (con defaults para el docker-compose local).
 */
public final class ConfiguracionPersistencia {

    private static final String UNIDAD_PERSISTENCIA = "skillswapPU";

    private final String url;
    private final String usuario;
    private final String password;

    public ConfiguracionPersistencia(String url, String usuario, String password) {
        this.url = url;
        this.usuario = usuario;
        this.password = password;
    }

    public static ConfiguracionPersistencia desdeEntorno() {
        return new ConfiguracionPersistencia(
                entorno("SKILLSWAP_DB_URL", "jdbc:mysql://localhost:3306/skillswap"),
                entorno("SKILLSWAP_DB_USER", "skillswap"),
                entorno("SKILLSWAP_DB_PASSWORD", "skillswap"));
    }

    /** Aplica las migraciones versionadas de src/main/resources/db/migration. */
    public void migrar() {
        Flyway.configure()
                .dataSource(url, usuario, password)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    public EntityManagerFactory crearEntityManagerFactory() {
        Map<String, Object> propiedades = Map.of(
                "jakarta.persistence.jdbc.url", url,
                "jakarta.persistence.jdbc.user", usuario,
                "jakarta.persistence.jdbc.password", password);
        return Persistence.createEntityManagerFactory(UNIDAD_PERSISTENCIA, propiedades);
    }

    private static String entorno(String variable, String porDefecto) {
        String valor = System.getenv(variable);
        return valor == null || valor.isBlank() ? porDefecto : valor;
    }
}
