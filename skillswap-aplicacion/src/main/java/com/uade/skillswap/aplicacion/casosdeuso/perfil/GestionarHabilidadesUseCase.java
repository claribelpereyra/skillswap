package com.uade.skillswap.aplicacion.casosdeuso.perfil;

import com.uade.skillswap.aplicacion.puertos.salida.PerfilRepository;
import com.uade.skillswap.dominio.perfil.Habilidad;
import com.uade.skillswap.dominio.perfil.Perfil;

import java.util.Objects;

/**
 * Caso de uso: Administrar habilidades técnicas e intereses del perfil (§4.1).
 */
public class GestionarHabilidadesUseCase {

    private final PerfilRepository perfilRepository;

    public GestionarHabilidadesUseCase(PerfilRepository perfilRepository) {
        this.perfilRepository = Objects.requireNonNull(perfilRepository, "El repositorio no puede ser nulo.");
    }

    public Perfil agregarHabilidad(String userId, String nombreHabilidad) {
        Perfil perfil = obtenerPerfil(userId);
        perfil.agregarHabilidad(new Habilidad(nombreHabilidad));
        return perfilRepository.guardar(perfil);
    }

    public Perfil quitarHabilidad(String userId, String nombreHabilidad) {
        Perfil perfil = obtenerPerfil(userId);
        perfil.quitarHabilidad(new Habilidad(nombreHabilidad));
        return perfilRepository.guardar(perfil);
    }

    public Perfil agregarInteres(String userId, String interes) {
        Perfil perfil = obtenerPerfil(userId);
        perfil.agregarInteres(interes);
        return perfilRepository.guardar(perfil);
    }

    public Perfil quitarInteres(String userId, String interes) {
        Perfil perfil = obtenerPerfil(userId);
        perfil.quitarInteres(interes);
        return perfilRepository.guardar(perfil);
    }

    private Perfil obtenerPerfil(String userId) {
        return perfilRepository.buscarPorUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No existe perfil registrado para: " + userId));
    }
}
