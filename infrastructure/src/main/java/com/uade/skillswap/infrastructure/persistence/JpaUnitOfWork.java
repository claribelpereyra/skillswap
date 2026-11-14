package com.uade.skillswap.infrastructure.persistence;

import com.uade.skillswap.application.ports.out.UnitOfWork;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

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
            throw error;
        } finally {
            EntityManagerContext.releaseCurrent();
        }
    }
}
