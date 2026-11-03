package com.uade.skillswap.aplicacion.casosdeuso.perfil;

import com.uade.skillswap.aplicacion.puertos.salida.ConsultaPublicacionesActivas;
import com.uade.skillswap.aplicacion.puertos.salida.ConsultaPublicacionesActivas.PublicacionResumenDto;
import com.uade.skillswap.aplicacion.puertos.salida.PerfilRepository;
import com.uade.skillswap.dominio.perfil.Disponibilidad;
import com.uade.skillswap.dominio.perfil.Habilidad;
import com.uade.skillswap.dominio.perfil.Perfil;
import com.uade.skillswap.dominio.perfil.TipoCuenta;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Fachada (Patrón GoF Facade - §4.1, §7) para la consulta del Perfil Público.
 * <p>
 * Unifica en una interfaz simple y de alto nivel subsistemas y módulos desacoplados:
 * - Datos del perfil de usuario (nombre, biografía, habilidades).
 * - Reputación acumulada y cantidad de calificaciones verificadas.
 * - Publicaciones activas vigentes provistas por el catálogo de publicaciones.
 */
public class ConsultarPerfilPublicoFacade {

    public record PerfilPublicoDto(
            String userId,
            String nombreVisible,
            String biografia,
            TipoCuenta tipoCuenta,
            double reputacion,
            int cantidadCalificaciones,
            Set<Habilidad> habilidades,
            Disponibilidad disponibilidad,
            boolean activo,
            List<PublicacionResumenDto> publicacionesActivas
    ) {}

    private final PerfilRepository perfilRepository;
    private final ConsultaPublicacionesActivas consultaPublicaciones;

    public ConsultarPerfilPublicoFacade(PerfilRepository perfilRepository,
                                        ConsultaPublicacionesActivas consultaPublicaciones) {
        this.perfilRepository = Objects.requireNonNull(perfilRepository, "El repositorio de perfiles no puede ser nulo.");
        this.consultaPublicaciones = Objects.requireNonNull(consultaPublicaciones, "La consulta de publicaciones no puede ser nula.");
    }

    /**
     * Entrega una vista pública completa del usuario sin exponer dependencias cruzadas.
     *
     * @param userId identificador del usuario a consultar.
     * @return PerfilPublicoDto consolidado con datos, reputación y ofertas activas.
     */
    public PerfilPublicoDto consultar(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("El userId no puede ser nulo ni vacío.");
        }

        Perfil perfil = perfilRepository.buscarPorUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Perfil no encontrado para el usuario: " + userId));

        List<PublicacionResumenDto> publicaciones = consultaPublicaciones.obtenerPublicacionesActivasDeAutor(userId);

        return new PerfilPublicoDto(
                perfil.getUserId(),
                perfil.getNombreVisible(),
                perfil.getBiografia(),
                perfil.getTipoCuenta(),
                perfil.getReputacion(),
                perfil.getCantidadCalificaciones(),
                perfil.getHabilidades(),
                perfil.getDisponibilidad(),
                perfil.estaActivo(),
                publicaciones != null ? publicaciones : List.of()
        );
    }
}
