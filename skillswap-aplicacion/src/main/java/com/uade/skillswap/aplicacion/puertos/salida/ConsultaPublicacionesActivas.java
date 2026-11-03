package com.uade.skillswap.aplicacion.puertos.salida;

import java.util.List;

/**
 * Puerto de salida para consultar las publicaciones activas de un autor.
 * Utilizado por el Facade de Perfil Público para unificar la vista sin acoplar
 * el módulo de perfiles al módulo de publicaciones (§4.1, §7).
 */
public interface ConsultaPublicacionesActivas {

    record PublicacionResumenDto(Long id, String titulo, String tipo, long precioSkillCoins) {}

    List<PublicacionResumenDto> obtenerPublicacionesActivasDeAutor(String autorUserId);
}
