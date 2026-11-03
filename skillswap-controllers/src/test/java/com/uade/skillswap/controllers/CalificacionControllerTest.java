package com.uade.skillswap.controllers;

import com.uade.skillswap.aplicacion.casosdeuso.calificacion.ConsultarCalificacionesUseCase;
import com.uade.skillswap.aplicacion.casosdeuso.calificacion.RegistrarCalificacionUseCase;
import com.uade.skillswap.aplicacion.seguridad.AccesoDenegadoException;
import com.uade.skillswap.aplicacion.seguridad.ValidadorPermisos;
import com.uade.skillswap.dominio.calificacion.Calificacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalificacionControllerTest {

    @Mock private RegistrarCalificacionUseCase registrarCalificacionUseCase;
    @Mock private ConsultarCalificacionesUseCase consultarCalificacionesUseCase;
    @Mock private ValidadorPermisos validadorPermisos;

    @InjectMocks
    private CalificacionController controller;

    @Test
    @DisplayName("Registra calificación validando permisos del autor y delegando en caso de uso")
    void registraCalificacionExitosamente() {
        Calificacion mockCalificacion = new Calificacion(1L, "usr-autor", "usr-dest", 5, "Genial");
        when(registrarCalificacionUseCase.ejecutar(any())).thenReturn(mockCalificacion);

        CalificacionController.RegistrarCalificacionRequest req = new CalificacionController.RegistrarCalificacionRequest(
                1L, "usr-autor", "usr-dest", 5, "Genial"
        );

        Calificacion resultado = controller.registrarCalificacion(req);

        assertThat(resultado.getPuntaje()).isEqualTo(5);
        verify(validadorPermisos).validarEsPropietario("usr-autor");
        verify(registrarCalificacionUseCase).ejecutar(any());
    }

    @Test
    @DisplayName("Rechaza calificación si el usuario en sesión no es el autor")
    void rechazaSiNoEsAutor() {
        doThrow(new AccesoDenegadoException("Acceso denegado"))
                .when(validadorPermisos).validarEsPropietario("usr-autor");

        CalificacionController.RegistrarCalificacionRequest req = new CalificacionController.RegistrarCalificacionRequest(
                1L, "usr-autor", "usr-dest", 5, "Genial"
        );

        assertThatThrownBy(() -> controller.registrarCalificacion(req))
                .isInstanceOf(AccesoDenegadoException.class);

        verify(registrarCalificacionUseCase, never()).ejecutar(any());
    }

    @Test
    @DisplayName("Lista calificaciones de un usuario correctamente")
    void listaCalificacionesDeUsuario() {
        when(consultarCalificacionesUseCase.listarPorDestinatario("usr-1"))
                .thenReturn(List.of(new Calificacion(1L, "usr-2", "usr-1", 4, "Bueno")));

        List<Calificacion> lista = controller.obtenerCalificacionesDeUsuario("usr-1");

        assertThat(lista).hasSize(1);
        verify(consultarCalificacionesUseCase).listarPorDestinatario("usr-1");
    }
}
