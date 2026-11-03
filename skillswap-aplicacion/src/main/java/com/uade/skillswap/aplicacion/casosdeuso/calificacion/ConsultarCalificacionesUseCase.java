package com.uade.skillswap.aplicacion.casosdeuso.calificacion;

import com.uade.skillswap.aplicacion.puertos.salida.CalificacionRepository;
import com.uade.skillswap.dominio.calificacion.Calificacion;

import java.util.List;
import java.util.Objects;

/**
 * Caso de uso: Consultar el historial de calificaciones recibidas por un perfil (§4.4).
 */
public class ConsultarCalificacionesUseCase {

    private final CalificacionRepository calificacionRepository;

    public ConsultarCalificacionesUseCase(CalificacionRepository calificacionRepository) {
        this.calificacionRepository = Objects.requireNonNull(calificacionRepository, "El repositorio no puede ser nulo.");
    }

    public List<Calificacion> listarPorDestinatario(String destinatarioId) {
        if (destinatarioId == null || destinatarioId.trim().isEmpty()) {
            throw new IllegalArgumentException("El destinatario no puede ser nulo ni vacío.");
        }
        return calificacionRepository.buscarPorDestinatario(destinatarioId.trim());
    }
}
