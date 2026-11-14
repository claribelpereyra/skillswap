package com.uade.skillswap.infraestructura.persistencia.notificaciones;

import com.uade.skillswap.aplicacion.notificaciones.PaginaNotificaciones;
import com.uade.skillswap.aplicacion.puertos.salida.RepositorioNotificaciones;
import com.uade.skillswap.dominio.notificaciones.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

public final class RepositorioNotificacionesJpa implements RepositorioNotificaciones {
    private final OperacionJpaNotificaciones operaciones;

    public RepositorioNotificacionesJpa(EntityManagerFactory fabrica) {
        operaciones = new OperacionJpaNotificaciones(fabrica);
    }

    @Override public boolean registrarSiAusente(Notificacion notificacion) {
        // La restricción única protege incluso si dos consumidores intentan insertar a la vez.
        try {
            return operaciones.ejecutar(em -> {
                em.persist(MapperNotificaciones.aJpa(notificacion));
                em.flush();
                return true;
            });
        } catch (PersistenceException fallo) {
            boolean existente = operaciones.ejecutar(em -> em.createQuery(
                    "select count(n) from NotificacionJpaEntity n where n.eventoId = :evento and n.destinatario = :usuario", Long.class)
                    .setParameter("evento", notificacion.aviso().eventoId().toString())
                    .setParameter("usuario", notificacion.aviso().destinatario()).getSingleResult() > 0);
            if (existente) return false;
            throw fallo;
        }
    }

    @Override public PaginaNotificaciones listar(String destinatario, int pagina, int tamanio) {
        PaginaNotificaciones.validar(pagina, tamanio);
        return operaciones.ejecutar(em -> {
            long total = em.createQuery("select count(n) from NotificacionJpaEntity n where n.destinatario = :usuario", Long.class)
                    .setParameter("usuario", destinatario).getSingleResult();
            var elementos = em.createQuery("select n from NotificacionJpaEntity n where n.destinatario = :usuario "
                    + "order by n.ocurridoEn desc, n.id desc", NotificacionJpaEntity.class)
                    .setParameter("usuario", destinatario).setFirstResult(pagina * tamanio).setMaxResults(tamanio)
                    .getResultList().stream().map(MapperNotificaciones::aDominio).toList();
            return new PaginaNotificaciones(elementos, pagina, tamanio, total);
        });
    }

    @Override public boolean marcarLeida(String destinatario, UUID id, Instant instante) {
        Objects.requireNonNull(instante);
        return operaciones.ejecutar(em -> {
            // Lock evita perder la fecha de la primera lectura ante dos marcas simultáneas.
            var entidad = em.find(NotificacionJpaEntity.class, id.toString(), LockModeType.PESSIMISTIC_WRITE);
            if (entidad == null || !entidad.destinatario.equals(destinatario)) return false;
            if (entidad.leidaEn == null) entidad.leidaEn = instante;
            return true;
        });
    }

    @Override public void registrarEntrega(String destinatario, UUID id, EstadoEntrega estado) {
        Objects.requireNonNull(estado);
        if (estado == EstadoEntrega.PENDIENTE) throw new IllegalArgumentException("Falta resultado de entrega");
        operaciones.ejecutar(em -> {
            var entidad = em.find(NotificacionJpaEntity.class, id.toString(), LockModeType.PESSIMISTIC_WRITE);
            if (entidad == null || !entidad.destinatario.equals(destinatario)) {
                throw new IllegalArgumentException("No se encontró la notificación del usuario");
            }
            entidad.entrega = estado;
            return null;
        });
    }
}
