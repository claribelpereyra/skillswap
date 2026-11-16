package com.uade.skillswap.infrastructure.persistence;

import com.uade.skillswap.application.common.ConcurrentUpdateException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JpaUnitOfWorkTest {

    private static final PersistenceConfiguration H2 = new PersistenceConfiguration(
            "jdbc:h2:mem:unit-of-work;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");

    private final JpaUnitOfWork unitOfWork = new JpaUnitOfWork();

    @BeforeAll
    static void startDatabase() {
        H2.migrate();
        EntityManagerContext.init(H2.createEntityManagerFactory());
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
    void translatesAnOptimisticLockConflictAndRollsBack() {
        assertThatThrownBy(() -> unitOfWork.execute(() -> {
            insert("test-a");
            throw new RollbackException(new OptimisticLockException("stale version"));
        })).isExactlyInstanceOf(ConcurrentUpdateException.class)
                .hasMessage("The data was updated by someone else at the same time, please try again");

        assertThat(countTestRows()).isZero();
    }

    @Test
    void initializingAgainClosesThePreviousFactory() {
        EntityManagerFactory previous = EntityManagerContext.withEntityManager(EntityManager::getEntityManagerFactory);

        EntityManagerContext.init(H2.createEntityManagerFactory());

        assertThat(previous.isOpen()).isFalse();
        unitOfWork.execute(() -> insert("test-a"));
        assertThat(countTestRows()).isEqualTo(1);
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

    @Test
    void nestedCallbacksRunOnceAfterTheRootCommitAndCanReadCommittedData() {
        AtomicInteger called = new AtomicInteger();
        unitOfWork.execute(() -> {
            insert("test-committed");
            unitOfWork.execute(() -> unitOfWork.afterCommit(() -> {
                assertThat(EntityManagerContext.hasCurrent()).isFalse();
                assertThat(countTestRows()).isEqualTo(1);
                called.incrementAndGet();
            }));
            assertThat(called.get()).isZero();
        });
        assertThat(called.get()).isEqualTo(1);
    }

    @Test
    void rootRollbackDiscardsNestedCallbacksAndDoesNotLeakToTheNextTransaction() {
        AtomicInteger called = new AtomicInteger();
        assertThatThrownBy(() -> unitOfWork.execute(() -> {
            insert("test-rollback");
            unitOfWork.execute(() -> unitOfWork.afterCommit(called::incrementAndGet));
            throw new IllegalStateException("rollback");
        })).isExactlyInstanceOf(IllegalStateException.class).hasMessage("rollback");
        unitOfWork.execute(() -> insert("test-next"));
        assertThat(called.get()).isZero();
        assertThat(countTestRows()).isEqualTo(1);
    }

    @Test
    void rejectsOrphanCallbacksAndContinuesAfterAFailedCallback() {
        assertThatThrownBy(() -> unitOfWork.afterCommit(() -> { }))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("afterCommit requires an active unit of work");
        AtomicInteger called = new AtomicInteger();
        unitOfWork.execute(() -> {
            insert("test-callback");
            unitOfWork.afterCommit(() -> { throw new IllegalStateException("delivery unavailable"); });
            unitOfWork.afterCommit(called::incrementAndGet);
        });
        assertThat(called.get()).isEqualTo(1);
        assertThat(countTestRows()).isEqualTo(1);
    }

    private static long countTestRows() {
        return EntityManagerContext.withEntityManager(em -> ((Number) em
                .createNativeQuery("SELECT COUNT(*) FROM info_esquema WHERE clave LIKE 'test-%'")
                .getSingleResult()).longValue());
    }
}
