package com.uade.skillswap.aplicacion.puertos.salida;

import com.uade.skillswap.dominio.calificacion.Calificacion;

import java.util.List;

/**
 * Puerto de salida para persistir y consultar calificaciones verificadas (§4.4, §8).
 */
public interface CalificacionRepository {

    Calificacion guardar(Calificacion calificacion);

    List<Calificacion> buscarPorDestinatario(String destinatarioId);

    /**
     * Verifica si un autor ya emitió una calificación para una contratación y destinatario específicos.
     * Garantiza la regla de negocio de calificación única por parte (§4.4, §5).
     */
    boolean yaCalifico(Long contratacionId, String autorId, String destinatarioId);
}
