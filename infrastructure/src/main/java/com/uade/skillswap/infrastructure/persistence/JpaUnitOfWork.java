package com.uade.skillswap.infrastructure.persistence;

import com.uade.skillswap.application.common.ConcurrentUpdateException;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.OptimisticLockException;

import java.util.function.Supplier;

public class JpaUnitOfWork implements UnitOfWork {

    @Override
    public <T> T execute(Supplier<T> work) {
        // Si ya hay una unidad de trabajo activa, se suma a esa transacción.
        if (EntityManagerContext.hasCurrent()) {
            return work.get();
        }
        EntityManager entityManager = EntityManagerContext.openCurrent();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            T result = work.get();
            transaction.commit();
            return result;
        } catch (RuntimeException error) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            if (causedByOptimisticLock(error)) {
                throw new ConcurrentUpdateException(error);
            }
            throw error;
        } finally {
            EntityManagerContext.releaseCurrent();
        }
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
