package com.uade.skillswap.aplicacion.casosdeuso.perfil;

import com.uade.skillswap.aplicacion.puertos.salida.PerfilRepository;
import com.uade.skillswap.dominio.perfil.Perfil;

import java.util.Objects;

/**
 * Caso de uso: Dar de baja lógica un perfil de usuario (§5).
 * Preserva todo el historial transaccional pero bloquea nuevas actividades.
 */
public class DarDeBajaPerfilUseCase {

    private final PerfilRepository perfilRepository;

    public DarDeBajaPerfilUseCase(PerfilRepository perfilRepository) {
        this.perfilRepository = Objects.requireNonNull(perfilRepository, "El repositorio no puede ser nulo.");
    }

    public void ejecutar(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("El userId no puede ser nulo ni vacío.");
        }

        Perfil perfil = perfilRepository.buscarPorUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No existe perfil registrado para: " + userId));

        perfil.desactivar();
        perfilRepository.guardar(perfil);
    }
}
