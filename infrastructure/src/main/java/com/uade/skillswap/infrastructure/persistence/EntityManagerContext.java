package com.uade.skillswap.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.Objects;
import java.util.function.Function;

// Singleton a propósito: una sola EntityManagerFactory por app y el EntityManager de cada hilo en un ThreadLocal.
public final class EntityManagerContext {

    private static EntityManagerFactory factory;
    private static final ThreadLocal<EntityManager> CURRENT = new ThreadLocal<>();

    private EntityManagerContext() {
    }

    public static void init(EntityManagerFactory entityManagerFactory) {
        Objects.requireNonNull(entityManagerFactory, "The EntityManagerFactory is required");
        if (factory != entityManagerFactory) {
            close();
        }
        factory = entityManagerFactory;
    }

    public static void close() {
        if (factory != null && factory.isOpen()) {
            factory.close();
        }
    }

    // Dentro de una unidad de trabajo usa su EntityManager; fuera, abre uno corto y lo cierra.
    public static <T> T withEntityManager(Function<EntityManager, T> work) {
        EntityManager current = CURRENT.get();
        if (current != null) {
            return work.apply(current);
        }
        EntityManager shortLived = factory().createEntityManager();
        try {
            return work.apply(shortLived);
        } finally {
            shortLived.close();
        }
    }

    static boolean hasCurrent() {
        return CURRENT.get() != null;
    }

    static EntityManager openCurrent() {
        EntityManager entityManager = factory().createEntityManager();
        CURRENT.set(entityManager);
        return entityManager;
    }

    static void releaseCurrent() {
        EntityManager entityManager = CURRENT.get();
        CURRENT.remove();
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
    }

    private static EntityManagerFactory factory() {
        if (factory == null) {
            throw new IllegalStateException("EntityManagerContext has not been initialized");
        }
        return factory;
    }
}
