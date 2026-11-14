package com.uade.skillswap.infrastructure.persistence;

import com.uade.skillswap.application.ports.out.UnitOfWork;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.function.Function;

/**
 * Base para los tests de persistencia de todas las áreas.
 *
 * <ul>
 *   <li>Levanta una H2 en memoria en modo MySQL y le aplica las mismas migraciones de Flyway que usa la app,
 *       así el esquema de los tests es el real (y Hibernate lo valida contra las entidades).</li>
 *   <li>La base y la EntityManagerFactory se crean una sola vez por corrida y las comparten todas las clases.</li>
 *   <li>Antes de cada test vacía todas las tablas (salvo el historial de Flyway): cada test arma sus
 *       propios datos y no depende del orden ni de lo que dejó otro test. Esto incluye los datos de ejemplo
 *       de las migraciones: si un test los necesita, los inserta él.</li>
 * </ul>
 *
 * Uso:
 * <pre>
 * class PublicationJpaRepositoryTest extends PersistenceTestBase {
 *
 *     &#64;Test
 *     void guardaYRecupera() {
 *         sql("INSERT INTO categories (id, name) VALUES (?, ?)", 1, "Tecnología");
 *         Publication saved = unitOfWork.execute(() -> repository.save(draft));
 *         assertThat(count("publications")).isEqualTo(1);
 *     }
 * }
 * </pre>
 */
public abstract class PersistenceTestBase {

    // MODE=MySQL y DATABASE_TO_LOWER para parecerse a MySQL (nombres en minúscula, sintaxis compatible).
    // DB_CLOSE_DELAY=-1 mantiene la base viva mientras dure la JVM aunque no haya conexiones abiertas.
    private static final String URL = "jdbc:h2:mem:skillswap-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    private static EntityManagerFactory factory;

    protected final UnitOfWork unitOfWork = new JpaUnitOfWork();

    @BeforeAll
    static void startDatabase() {
        if (factory == null || !factory.isOpen()) {
            PersistenceConfiguration h2 = new PersistenceConfiguration(URL, "sa", "");
            h2.migrate();
            factory = h2.createEntityManagerFactory();
        }
        // Se inicializa en cada clase por si otro test dejó el contexto apuntando a otra factory.
        EntityManagerContext.init(factory);
    }

    @BeforeEach
    void emptyAllTables() {
        inTransaction(em -> {
            @SuppressWarnings("unchecked")
            List<String> tables = em.createNativeQuery("""
                            SELECT table_name FROM information_schema.tables
                            WHERE table_schema = CURRENT_SCHEMA AND table_type = 'BASE TABLE'
                              AND table_name <> 'flyway_schema_history'
                            """)
                    .getResultList();
            em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
            for (String table : tables) {
                em.createNativeQuery("TRUNCATE TABLE " + table + " RESTART IDENTITY").executeUpdate();
            }
            em.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
            return null;
        });
    }

    // Corre el trabajo dentro de una transacción (la misma unidad de trabajo que usan los casos de uso).
    protected <T> T inTransaction(Function<EntityManager, T> work) {
        return unitOfWork.execute(() -> EntityManagerContext.withEntityManager(work));
    }

    // Ejecuta un INSERT/UPDATE/DELETE nativo para preparar datos. Los parámetros van en orden (?1, ?2...).
    protected int sql(String statement, Object... parameters) {
        return inTransaction(em -> {
            Query query = em.createNativeQuery(numbered(statement));
            for (int i = 0; i < parameters.length; i++) {
                query.setParameter(i + 1, parameters[i]);
            }
            return query.executeUpdate();
        });
    }

    protected long count(String table) {
        return inTransaction(em -> ((Number) em.createNativeQuery("SELECT COUNT(*) FROM " + table)
                .getSingleResult()).longValue());
    }

    // Permite escribir "?" sin número, como en JDBC, y lo traduce a ?1, ?2... para JPA.
    private static String numbered(String statement) {
        StringBuilder result = new StringBuilder();
        int next = 1;
        for (int i = 0; i < statement.length(); i++) {
            char c = statement.charAt(i);
            boolean alreadyNumbered = i + 1 < statement.length() && Character.isDigit(statement.charAt(i + 1));
            if (c == '?' && !alreadyNumbered) {
                result.append('?').append(next++);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
