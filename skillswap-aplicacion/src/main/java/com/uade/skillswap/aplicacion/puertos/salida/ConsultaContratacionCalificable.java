package com.uade.skillswap.aplicacion.puertos.salida;

/**
 * Puerto de salida para verificar el estado de una contratación antes de permitir una calificación (§4.4, §5).
 * Resuelve el desacoplamiento entre el módulo de contrataciones (Clari) y el de calificaciones (Andy).
 */
public interface ConsultaContratacionCalificable {

    /**
     * Verifica si la contratación existe y se encuentra en estado COMPLETADA.
     */
    boolean esContratacionCompletada(Long contratacionId);

    /**
     * Verifica si el usuario especificado participó como cliente o proveedor en la contratación.
     */
    boolean participoEnContratacion(Long contratacionId, String userId);

    /**
     * Obtiene el identificador del otro participante (la contraparte) en la contratación.
     */
    String obtenerContraparte(Long contratacionId, String userId);
}
