package com.uade.skillswap.infraestructura.adaptadores;

import com.uade.skillswap.aplicacion.puertos.salida.ServicioIdentidad;

import java.util.Optional;

/**
 * Adaptador (Patrón GoF Adapter - §7, §10) que implementa el puerto {@link ServicioIdentidad}
 * simulando un proveedor de identidad y contexto de sesión en memoria.
 * <p>
 * Permite alternar el usuario activo durante los tests o en la interfaz local sin requerir
 * autenticación real (OAuth / JWT), desacoplando los controladores de la capa de seguridad.
 */
public class MockServicioIdentidadAdapter implements ServicioIdentidad {

    private String userIdActivo;

    public MockServicioIdentidadAdapter() {
        this.userIdActivo = null;
    }

    public MockServicioIdentidadAdapter(String userIdInicial) {
        this.userIdActivo = userIdInicial != null ? userIdInicial.trim() : null;
    }

    /**
     * Simula el inicio de sesión de un usuario con su identificador externo.
     *
     * @param userId identificador del usuario.
     */
    public void simularLogin(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("El userId a simular no puede ser nulo ni vacío.");
        }
        this.userIdActivo = userId.trim();
    }

    /**
     * Simula el cierre de sesión del usuario activo.
     */
    public void simularLogout() {
        this.userIdActivo = null;
    }

    @Override
    public String obtenerUserIdActivo() {
        if (userIdActivo == null) {
            throw new IllegalStateException("No hay ningún usuario autenticado en la sesión actual.");
        }
        return userIdActivo;
    }

    @Override
    public Optional<String> getUserIdActivoOpcional() {
        return Optional.ofNullable(userIdActivo);
    }

    @Override
    public boolean hayUsuarioAutenticado() {
        return userIdActivo != null;
    }
}
