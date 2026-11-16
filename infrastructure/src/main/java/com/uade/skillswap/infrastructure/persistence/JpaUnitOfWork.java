package com.uade.skillswap.infrastructure.persistence;

import com.uade.skillswap.application.common.ConcurrentUpdateException;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.OptimisticLockException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JpaUnitOfWork implements UnitOfWork {
    private static final Logger LOG = Logger.getLogger(JpaUnitOfWork.class.getName());

    // Callbacks que esperan a que la transacción raíz confirme. Son por hilo porque el EntityManager también lo es.
    private static final ThreadLocal<List<Runnable>> AFTER_COMMIT = new ThreadLocal<>();

    @Override
    public <T> T execute(Supplier<T> work) {
        // Si ya hay una unidad de trabajo activa, se suma a esa transacción y los afterCommit los corre la raíz.
        if (EntityManagerContext.hasCurrent()) {
            return work.get();
        }
        EntityManager entityManager = EntityManagerContext.openCurrent();
        EntityTransaction transaction = entityManager.getTransaction();
        AFTER_COMMIT.set(new ArrayList<>());
        List<Runnable> callbacks;
        T result;
        try {
            transaction.begin();
            result = work.get();
            transaction.commit();
        } catch (RuntimeException | Error error) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            AFTER_COMMIT.remove();
            if (causedByOptimisticLock(error)) {
                throw new ConcurrentUpdateException(error);
            }
            throw error;
        } finally {
            EntityManagerContext.releaseCurrent();
        }
        callbacks = AFTER_COMMIT.get();
        AFTER_COMMIT.remove();
        for (Runnable callback : callbacks) {
            try {
                callback.run();
            } catch (RuntimeException error) {
                LOG.log(Level.WARNING, "After-commit callback failed; business transaction remains committed", error);
            }
        }
        return result;
    }

    @Override
    public void afterCommit(Runnable callback) {
        if (!EntityManagerContext.hasCurrent() || AFTER_COMMIT.get() == null) {
            throw new IllegalStateException("afterCommit requires an active unit of work");
        }
        AFTER_COMMIT.get().add(Objects.requireNonNull(callback, "The callback is required"));
    }

    // El choque de versiones puede salir en un flush o en el commit, envuelto en RollbackException.
    private static boolean causedByOptimisticLock(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof OptimisticLockException) {
                return true;
            }
        }
        return false;
    }
}
