package com.uade.skillswap.controllers;

import com.uade.skillswap.aplicacion.casosdeuso.calificacion.ConsultarCalificacionesUseCase;
import com.uade.skillswap.aplicacion.casosdeuso.calificacion.RegistrarCalificacionUseCase;
import com.uade.skillswap.aplicacion.seguridad.ValidadorPermisos;
import com.uade.skillswap.dominio.calificacion.Calificacion;

import java.util.List;
import java.util.Objects;

/**
 * Controlador de Presentación para el registro y consulta de Calificaciones Verificadas (§9).
 * <p>
 * Valida la identidad del autor mediante el {@link ValidadorPermisos} y delega en los casos de uso.
 */
public class CalificacionController {

    public record RegistrarCalificacionRequest(
            Long contratacionId,
            String autorId,
            String destinatarioId,
            int puntaje,
            String comentario
    ) {}

    private final RegistrarCalificacionUseCase registrarCalificacionUseCase;
    private final ConsultarCalificacionesUseCase consultarCalificacionesUseCase;
    private final ValidadorPermisos validadorPermisos;

    public CalificacionController(RegistrarCalificacionUseCase registrarCalificacionUseCase,
                                  ConsultarCalificacionesUseCase consultarCalificacionesUseCase,
                                  ValidadorPermisos validadorPermisos) {
        this.registrarCalificacionUseCase = Objects.requireNonNull(registrarCalificacionUseCase);
        this.consultarCalificacionesUseCase = Objects.requireNonNull(consultarCalificacionesUseCase);
        this.validadorPermisos = Objects.requireNonNull(validadorPermisos);
    }

    public Calificacion registrarCalificacion(RegistrarCalificacionRequest request) {
        Objects.requireNonNull(request, "La solicitud de calificación no puede ser nula.");
        validadorPermisos.validarEsPropietario(request.autorId());

        return registrarCalificacionUseCase.ejecutar(new RegistrarCalificacionUseCase.Command(
                request.contratacionId(),
                request.autorId(),
                request.destinatarioId(),
                request.puntaje(),
                request.comentario()
        ));
    }

    public List<Calificacion> obtenerCalificacionesDeUsuario(String userId) {
        return consultarCalificacionesUseCase.listarPorDestinatario(userId);
    }
}
