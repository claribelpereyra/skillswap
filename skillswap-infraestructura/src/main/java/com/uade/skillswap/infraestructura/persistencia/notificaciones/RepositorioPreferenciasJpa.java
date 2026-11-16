package com.uade.skillswap.infraestructura.persistencia.notificaciones;

import com.uade.skillswap.aplicacion.puertos.salida.RepositorioPreferencias;
import com.uade.skillswap.dominio.notificaciones.PreferenciaNotificacion;
import jakarta.persistence.EntityManagerFactory;
import java.util.Optional;

public final class RepositorioPreferenciasJpa implements RepositorioPreferencias {
    private final OperacionJpaNotificaciones operaciones;

    public RepositorioPreferenciasJpa(EntityManagerFactory fabrica) {
        operaciones = new OperacionJpaNotificaciones(fabrica);
    }

    @Override public Optional<PreferenciaNotificacion> buscar(String userId) {
        return operaciones.ejecutar(em -> Optional.ofNullable(em.find(PreferenciaJpaEntity.class, userId))
                .map(MapperNotificaciones::aDominio));
    }

    @Override public void guardar(PreferenciaNotificacion preferencia) {
        operaciones.ejecutar(em -> em.merge(MapperNotificaciones.aJpa(preferencia)));
    }
}
