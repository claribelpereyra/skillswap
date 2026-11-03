package com.uade.skillswap.aplicacion.seguridad;

import com.uade.skillswap.aplicacion.puertos.salida.ServicioIdentidad;

import java.util.Objects;

/**
 * Componente transversal para validar permisos de acceso y autoría de operaciones (§5, §10).
 * <p>
 * Verifica que el actor autenticado en sesión coincida con el propietario del recurso modificado,
 * impidiendo que un usuario suplante la identidad de otro.
 */
public class ValidadorPermisos {

    private final ServicioIdentidad servicioIdentidad;

    public ValidadorPermisos(ServicioIdentidad servicioIdentidad) {
        this.servicioIdentidad = Objects.requireNonNull(servicioIdentidad, "El servicio de identidad no puede ser nulo.");
    }

    /**
     * Valida que exista una sesión activa y que el usuario logueado coincida con el userId indicado.
     *
     * @param userIdEsperado identificador del usuario que debe coincidir con la sesión.
     * @throws AccesoDenegadoException si no coincide o no hay usuario autenticado.
     */
    public void validarEsPropietario(String userIdEsperado) {
        if (!servicioIdentidad.hayUsuarioAutenticado()) {
            throw new AccesoDenegadoException("Operación no autorizada: No hay ninguna sesión activa.");
        }
        String userIdActivo = servicioIdentidad.obtenerUserIdActivo();
        if (!userIdActivo.equalsIgnoreCase(userIdEsperado)) {
            throw new AccesoDenegadoException("Acceso denegado: El usuario activo ('" + userIdActivo +
                    "') no tiene permisos sobre el perfil o recurso de '" + userIdEsperado + "'.");
        }
    }

    /**
     * Obtiene el identificador del usuario activo, validando que exista sesión.
     */
    public String obtenerUserIdAutenticado() {
        return servicioIdentidad.obtenerUserIdActivo();
    }
}
