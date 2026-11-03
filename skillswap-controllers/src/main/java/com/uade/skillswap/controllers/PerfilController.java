package com.uade.skillswap.controllers;

import com.uade.skillswap.aplicacion.casosdeuso.perfil.*;
import com.uade.skillswap.aplicacion.casosdeuso.perfil.ConsultarPerfilPublicoFacade.PerfilPublicoDto;
import com.uade.skillswap.aplicacion.seguridad.ValidadorPermisos;
import com.uade.skillswap.dominio.perfil.Perfil;
import com.uade.skillswap.dominio.perfil.TipoCuenta;

import java.time.ZoneId;
import java.util.Objects;

/**
 * Controlador de Presentación para el módulo de Perfiles (§9).
 * <p>
 * Diseñado bajo el patrón Controller de GRASP: actúa como intermediario delegado entre
 * la interfaz de usuario (Mock UI Kit) y los casos de uso de la capa de aplicación.
 * Valida permisos con el {@link ValidadorPermisos} antes de permitir mutaciones.
 */
public class PerfilController {

    public record CrearPerfilRequest(
            String userId,
            String nombreVisible,
            String biografia,
            ZoneId zonaHoraria,
            TipoCuenta tipoCuenta
    ) {}

    public record EditarPerfilRequest(
            String userId,
            String nuevoNombreVisible,
            String nuevaBiografia,
            ZoneId nuevaZonaHoraria
    ) {}

    private final CrearPerfilUseCase crearPerfilUseCase;
    private final EditarPerfilUseCase editarPerfilUseCase;
    private final DarDeBajaPerfilUseCase darDeBajaPerfilUseCase;
    private final GestionarHabilidadesUseCase gestionarHabilidadesUseCase;
    private final ConsultarPerfilPublicoFacade consultarPerfilPublicoFacade;
    private final ValidadorPermisos validadorPermisos;

    public PerfilController(CrearPerfilUseCase crearPerfilUseCase,
                            EditarPerfilUseCase editarPerfilUseCase,
                            DarDeBajaPerfilUseCase darDeBajaPerfilUseCase,
                            GestionarHabilidadesUseCase gestionarHabilidadesUseCase,
                            ConsultarPerfilPublicoFacade consultarPerfilPublicoFacade,
                            ValidadorPermisos validadorPermisos) {
        this.crearPerfilUseCase = Objects.requireNonNull(crearPerfilUseCase);
        this.editarPerfilUseCase = Objects.requireNonNull(editarPerfilUseCase);
        this.darDeBajaPerfilUseCase = Objects.requireNonNull(darDeBajaPerfilUseCase);
        this.gestionarHabilidadesUseCase = Objects.requireNonNull(gestionarHabilidadesUseCase);
        this.consultarPerfilPublicoFacade = Objects.requireNonNull(consultarPerfilPublicoFacade);
        this.validadorPermisos = Objects.requireNonNull(validadorPermisos);
    }

    public Perfil crearPerfil(CrearPerfilRequest request) {
        Objects.requireNonNull(request, "La solicitud de creación no puede ser nula.");
        validadorPermisos.validarEsPropietario(request.userId());

        return crearPerfilUseCase.ejecutar(new CrearPerfilUseCase.Command(
                request.userId(),
                request.nombreVisible(),
                request.biografia(),
                request.zonaHoraria(),
                request.tipoCuenta()
        ));
    }

    public Perfil editarPerfil(EditarPerfilRequest request) {
        Objects.requireNonNull(request, "La solicitud de edición no puede ser nula.");
        validadorPermisos.validarEsPropietario(request.userId());

        return editarPerfilUseCase.ejecutar(new EditarPerfilUseCase.Command(
                request.userId(),
                request.nuevoNombreVisible(),
                request.nuevaBiografia(),
                request.nuevaZonaHoraria()
        ));
    }

    public void darDeBajaPerfil(String userId) {
        validadorPermisos.validarEsPropietario(userId);
        darDeBajaPerfilUseCase.ejecutar(userId);
    }

    public Perfil agregarHabilidad(String userId, String habilidad) {
        validadorPermisos.validarEsPropietario(userId);
        return gestionarHabilidadesUseCase.agregarHabilidad(userId, habilidad);
    }

    public Perfil quitarHabilidad(String userId, String habilidad) {
        validadorPermisos.validarEsPropietario(userId);
        return gestionarHabilidadesUseCase.quitarHabilidad(userId, habilidad);
    }

    public PerfilPublicoDto obtenerPerfilPublico(String userId) {
        return consultarPerfilPublicoFacade.consultar(userId);
    }
}
