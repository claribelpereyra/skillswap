package com.uade.skillswap.controllers;

import com.uade.skillswap.aplicacion.casosdeuso.perfil.*;
import com.uade.skillswap.aplicacion.casosdeuso.perfil.ConsultarPerfilPublicoFacade.PerfilPublicoDto;
import com.uade.skillswap.aplicacion.seguridad.AccesoDenegadoException;
import com.uade.skillswap.aplicacion.seguridad.ValidadorPermisos;
import com.uade.skillswap.dominio.perfil.Disponibilidad;
import com.uade.skillswap.dominio.perfil.Perfil;
import com.uade.skillswap.dominio.perfil.TipoCuenta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerfilControllerTest {

    @Mock private CrearPerfilUseCase crearPerfilUseCase;
    @Mock private EditarPerfilUseCase editarPerfilUseCase;
    @Mock private DarDeBajaPerfilUseCase darDeBajaPerfilUseCase;
    @Mock private GestionarHabilidadesUseCase gestionarHabilidadesUseCase;
    @Mock private ConsultarPerfilPublicoFacade consultarPerfilPublicoFacade;
    @Mock private ValidadorPermisos validadorPermisos;

    @InjectMocks
    private PerfilController controller;

    @Test
    @DisplayName("Crea perfil delegando en el caso de uso tras validar permisos")
    void crearPerfilValidaPermisosYDelega() {
        Perfil.class.getName();
        Perfil mockPerfil = new Perfil("usr-1", "Carlos", "Bio", ZoneId.systemDefault(), TipoCuenta.PROFESIONAL);
        when(crearPerfilUseCase.ejecutar(any())).thenReturn(mockPerfil);

        PerfilController.CrearPerfilRequest req = new PerfilController.CrearPerfilRequest(
                "usr-1", "Carlos", "Bio", ZoneId.systemDefault(), TipoCuenta.PROFESIONAL
        );

        Perfil resultado = controller.crearPerfil(req);

        assertThat(resultado.getUserId()).isEqualTo("usr-1");
        verify(validadorPermisos).validarEsPropietario("usr-1");
        verify(crearPerfilUseCase).ejecutar(any());
    }

    @Test
    @DisplayName("Rechaza creación si el validador de permisos deniega el acceso")
    void rechazaSiValidadorDeniega() {
        doThrow(new AccesoDenegadoException("Acceso denegado"))
                .when(validadorPermisos).validarEsPropietario("usr-otro");

        PerfilController.CrearPerfilRequest req = new PerfilController.CrearPerfilRequest(
                "usr-otro", "Carlos", "Bio", ZoneId.systemDefault(), TipoCuenta.PROFESIONAL
        );

        assertThatThrownBy(() -> controller.crearPerfil(req))
                .isInstanceOf(AccesoDenegadoException.class);

        verify(crearPerfilUseCase, never()).ejecutar(any());
    }

    @Test
    @DisplayName("Obtiene perfil público consolidado mediante el Facade")
    void obtenerPerfilPublicoDelegaEnFacade() {
        PerfilPublicoDto mockDto = new PerfilPublicoDto(
                "usr-1", "Carlos", "Bio", TipoCuenta.PROFESIONAL, 4.8, 10,
                Set.of(), Disponibilidad.NO_ESPECIFICADA, true, List.of()
        );
        when(consultarPerfilPublicoFacade.consultar("usr-1")).thenReturn(mockDto);

        PerfilPublicoDto resultado = controller.obtenerPerfilPublico("usr-1");

        assertThat(resultado.userId()).isEqualTo("usr-1");
        assertThat(resultado.reputacion()).isEqualTo(4.8);
        verify(consultarPerfilPublicoFacade).consultar("usr-1");
    }
}
