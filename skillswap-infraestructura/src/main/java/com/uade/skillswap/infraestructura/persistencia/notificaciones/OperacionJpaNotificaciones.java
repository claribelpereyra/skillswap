package com.uade.skillswap.infraestructura.persistencia.notificaciones;

import jakarta.persistence.*;
import java.util.Objects;
import java.util.function.Function;

/** Transacciones locales de bandeja/preferencias, independientes del negocio ya confirmado. */
final class OperacionJpaNotificaciones {
    private final EntityManagerFactory fabrica;

    OperacionJpaNotificaciones(EntityManagerFactory fabrica) { this.fabrica = Objects.requireNonNull(fabrica); }

    <T> T ejecutar(Function<EntityManager, T> operacion) {
        try (var em = fabrica.createEntityManager()) {
            var tx = em.getTransaction();
            try {
                tx.begin();
                T resultado = operacion.apply(em);
                tx.commit();
                return resultado;
            } catch (RuntimeException fallo) {
                if (tx.isActive()) tx.rollback();
                throw fallo;
            }
        }
    }
}
