package com.uade.skillswap.infrastructure.persistence;

import com.uade.skillswap.application.ports.out.UnitOfWork;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.function.Function;

// Base de los tests de persistencia: H2 en modo MySQL con las migraciones reales, compartida por todas las
// clases, y tablas vacías antes de cada test. Uso y decisiones en el PR #13.
public abstract class PersistenceTestBase {

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
            // REFERENTIAL_INTEGRITY es un seteo de toda la base y no se deshace con rollback:
            // se vuelve a activar siempre, aunque falle un TRUNCATE.
            em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
            try {
                for (String table : tables) {
                    em.createNativeQuery("TRUNCATE TABLE " + table + " RESTART IDENTITY").executeUpdate();
                }
            } finally {
                em.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
            }
            return null;
        });
    }

    protected <T> T inTransaction(Function<EntityManager, T> work) {
        return unitOfWork.execute(() -> EntityManagerContext.withEntityManager(work));
    }

    // INSERT/UPDATE/DELETE nativo para preparar datos, con parámetros posicionales (?1, ?2...).
    protected int sql(String statement, Object... parameters) {
        return inTransaction(em -> {
            Query query = em.createNativeQuery(statement);
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
}
