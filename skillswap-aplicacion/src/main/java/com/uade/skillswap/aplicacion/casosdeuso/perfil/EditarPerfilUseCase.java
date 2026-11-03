package com.uade.skillswap.aplicacion.casosdeuso.perfil;

import com.uade.skillswap.aplicacion.puertos.salida.PerfilRepository;
import com.uade.skillswap.dominio.perfil.Perfil;

import java.time.ZoneId;
import java.util.Objects;

/**
 * Caso de uso: Modificar datos personales y biografía del perfil (§4.1).
 */
public class EditarPerfilUseCase {

    public record Command(
            String userId,
            String nuevoNombreVisible,
            String nuevaBiografia,
            ZoneId nuevaZonaHoraria
    ) {}

    private final PerfilRepository perfilRepository;

    public EditarPerfilUseCase(PerfilRepository perfilRepository) {
        this.perfilRepository = Objects.requireNonNull(perfilRepository, "El repositorio no puede ser nulo.");
    }

    public Perfil ejecutar(Command command) {
        Objects.requireNonNull(command, "El comando no puede ser nulo.");

        Perfil perfil = perfilRepository.buscarPorUserId(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("No existe perfil registrado para: " + command.userId()));

        perfil.actualizarDatos(command.nuevoNombreVisible(), command.nuevaBiografia(), command.nuevaZonaHoraria());
        return perfilRepository.guardar(perfil);
    }
}
