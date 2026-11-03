package com.uade.skillswap.aplicacion.puertos.salida;

import com.uade.skillswap.dominio.perfil.Perfil;

import java.util.Optional;

/**
 * Puerto de salida (Repository) para la persistencia y recuperación de agregados {@link Perfil}.
 * La implementación concreta reside en la capa de infraestructura (JPA/Hibernate).
 */
public interface PerfilRepository {

    Perfil guardar(Perfil perfil);

    Optional<Perfil> buscarPorUserId(String userId);

    Optional<Perfil> buscarPorId(Long id);

    boolean existePorUserId(String userId);
}
