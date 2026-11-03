package com.uade.skillswap.aplicacion.casosdeuso.perfil;

import com.uade.skillswap.aplicacion.puertos.salida.PerfilRepository;
import com.uade.skillswap.dominio.perfil.Perfil;
import com.uade.skillswap.dominio.perfil.TipoCuenta;

import java.time.ZoneId;
import java.util.Objects;

/**
 * Caso de uso: Registrar y dar de alta un nuevo perfil en el sistema (§4.1, §9).
 */
public class CrearPerfilUseCase {

    public record Command(
            String userId,
            String nombreVisible,
            String biografia,
            ZoneId zonaHoraria,
            TipoCuenta tipoCuenta
    ) {}

    private final PerfilRepository perfilRepository;

    public CrearPerfilUseCase(PerfilRepository perfilRepository) {
        this.perfilRepository = Objects.requireNonNull(perfilRepository, "El repositorio de perfiles no puede ser nulo.");
    }

    public Perfil ejecutar(Command command) {
        Objects.requireNonNull(command, "El comando de creación no puede ser nulo.");

        if (perfilRepository.existePorUserId(command.userId())) {
            throw new IllegalStateException("Ya existe un perfil registrado para el userId: " + command.userId());
        }

        Perfil nuevoPerfil = new Perfil(
                command.userId(),
                command.nombreVisible(),
                command.biografia(),
                command.zonaHoraria(),
                command.tipoCuenta()
        );

        return perfilRepository.guardar(nuevoPerfil);
    }
}
