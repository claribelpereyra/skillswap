package com.uade.skillswap.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JpaUnitOfWorkTest {

    private final JpaUnitOfWork unitOfWork = new JpaUnitOfWork();

    @BeforeAll
    static void startDatabase() {
        PersistenceConfiguration h2 = new PersistenceConfiguration(
                "jdbc:h2:mem:unit-of-work;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        h2.migrate();
        EntityManagerContext.init(h2.createEntityManagerFactory());
    }

    @AfterAll
    static void stopDatabase() {
        EntityManagerContext.close();
    }

    @BeforeEach
    void cleanTestRows() {
        unitOfWork.execute(() -> EntityManagerContext.withEntityManager(em ->
                em.createNativeQuery("DELETE FROM info_esquema WHERE clave LIKE 'test-%'").executeUpdate()));
    }

    @Test
    void commitsEverythingTogether() {
        unitOfWork.execute(() -> {
            insert("test-a");
            insert("test-b");
        });

        assertThat(countTestRows()).isEqualTo(2);
    }

    @Test
    void rollsBackEverythingWhenTheWorkFails() {
        assertThatThrownBy(() -> unitOfWork.execute(() -> {
            insert("test-a");
            insert("test-b");
            throw new IllegalStateException("failed halfway");
        })).isInstanceOf(IllegalStateException.class).hasMessage("failed halfway");

        assertThat(countTestRows()).isZero();
    }

    @Test
    void aNestedUnitOfWorkJoinsTheOuterOne() {
        AtomicReference<EntityManager> outer = new AtomicReference<>();
        AtomicReference<EntityManager> inner = new AtomicReference<>();

        unitOfWork.execute(() -> {
            outer.set(EntityManagerContext.withEntityManager(em -> em));
            unitOfWork.execute(() -> {
                inner.set(EntityManagerContext.withEntityManager(em -> em));
            });
        });

        assertThat(inner.get()).isSameAs(outer.get());
    }

    @Test
    void aFailureInsideANestedUnitOfWorkRollsBackTheOuterOneToo() {
        assertThatThrownBy(() -> unitOfWork.execute(() -> {
            insert("test-outer");
            unitOfWork.execute(() -> {
                insert("test-inner");
                throw new IllegalStateException("no capacity left");
            });
        })).isInstanceOf(IllegalStateException.class);

        assertThat(countTestRows()).isZero();
    }

    @Test
    void readsOutsideAUnitOfWorkWithAShortLivedEntityManager() {
        AtomicReference<EntityManager> used = new AtomicReference<>();

        long rows = EntityManagerContext.withEntityManager(em -> {
            used.set(em);
            return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM info_esquema").getSingleResult()).longValue();
        });

        assertThat(rows).isGreaterThanOrEqualTo(1);
        assertThat(used.get().isOpen()).isFalse();
        assertThat(EntityManagerContext.hasCurrent()).isFalse();
    }

    @Test
    void releasesTheEntityManagerWhenItFinishes() {
        AtomicReference<EntityManager> used = new AtomicReference<>();

        unitOfWork.execute(() -> used.set(EntityManagerContext.withEntityManager(em -> em)));

        assertThat(used.get().isOpen()).isFalse();
        assertThat(EntityManagerContext.hasCurrent()).isFalse();
    }

    private static void insert(String key) {
        EntityManagerContext.withEntityManager(em -> em
                .createNativeQuery("INSERT INTO info_esquema (clave, valor) VALUES (?1, ?2)")
                .setParameter(1, key)
                .setParameter(2, "value")
                .executeUpdate());
    }

    private static long countTestRows() {
        return EntityManagerContext.withEntityManager(em -> ((Number) em
                .createNativeQuery("SELECT COUNT(*) FROM info_esquema WHERE clave LIKE 'test-%'")
                .getSingleResult()).longValue());
    }
}
