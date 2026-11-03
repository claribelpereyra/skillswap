package com.uade.skillswap.aplicacion.puertos.salida;

import java.util.Optional;

/**
 * Puerto de salida para consultar la identidad del usuario actualmente autenticado en el sistema (§10).
 * <p>
 * Sigue el principio de Inversión de Dependencias (DIP): la capa de aplicación define la interfaz
 * que requiere para conocer al actor activo, mientras que la capa de infraestructura proporciona
 * la implementación concreta (mediante el patrón GoF Adapter).
 */
public interface ServicioIdentidad {

    /**
     * Retorna el identificador del usuario activo en la sesión actual.
     *
     * @return identificador único del usuario (userId).
     * @throws IllegalStateException si no hay ningún usuario autenticado.
     */
    String obtenerUserIdActivo();

    /**
     * Retorna el identificador del usuario activo en un Optional.
     *
     * @return Optional con el userId, o vacío si no hay sesión activa.
     */
    Optional<String> getUserIdActivoOpcional();

    /**
     * Indica si existe un usuario autenticado en la sesión actual.
     *
     * @return true si hay sesión activa, false en caso contrario.
     */
    boolean hayUsuarioAutenticado();
}
