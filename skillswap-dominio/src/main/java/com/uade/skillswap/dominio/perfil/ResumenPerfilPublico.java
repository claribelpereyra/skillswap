package com.uade.skillswap.dominio.perfil;

import java.util.Set;

/**
 * Resumen consolidado del perfil para consulta pública (soporte del patrón Facade).
 */
public record ResumenPerfilPublico(
        String userId,
        String nombreVisible,
        String biografia,
        TipoCuenta tipoCuenta,
        double reputacion,
        int cantidadCalificaciones,
        Set<Habilidad> habilidades,
        Disponibilidad disponibilidad,
        boolean activo
) {
}
