package com.uade.skillswap.controllers;

import com.uade.skillswap.application.notificaciones.*;
import com.uade.skillswap.application.ports.in.BandejaNotificacionEntrada;
import com.uade.skillswap.controllers.comun.CodigoError;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificacionControllerTest {
    @Test void usaElUsuarioActualEnCadaOperacionYDelega() {
        var entrada = mock(BandejaNotificacionEntrada.class);
        var usuario = new AtomicReference<>("ana");
        var controller = new NotificacionController(entrada, usuario::get);
        var pagina = new PaginaNotificaciones(List.of(), 0, 10, 0);
        when(entrada.listar("ana", 0, 10)).thenReturn(pagina);
        assertThat(controller.listar(0, 10).datos()).isEqualTo(pagina);
        usuario.set("bea");
        var id = UUID.randomUUID();
        assertThat(controller.marcarLeida(id).exitoso()).isTrue();
        verify(entrada).marcarLeida("bea", id);
    }

    @Test void sinSesionOEntradaInvalidaNoLlamaAlCasoDeUso() {
        var entrada = mock(BandejaNotificacionEntrada.class);
        var sinSesion = new NotificacionController(entrada, () -> null);
        assertThat(sinSesion.listar(0, 10).error().codigo()).isEqualTo(CodigoError.NO_AUTORIZADO);
        assertThat(sinSesion.marcarLeida(UUID.randomUUID()).error().codigo()).isEqualTo(CodigoError.NO_AUTORIZADO);
        var controller = new NotificacionController(entrada, () -> "ana");
        assertThat(controller.listar(-1, 10).error().codigo()).isEqualTo(CodigoError.VALIDACION);
        assertThat(controller.marcarLeida(null).error().codigo()).isEqualTo(CodigoError.VALIDACION);
        verifyNoInteractions(entrada);
    }

    @Test void noExponeUnaNotificacionAjena() {
        var entrada = mock(BandejaNotificacionEntrada.class);
        var id = UUID.randomUUID();
        doThrow(new NotificacionNoEncontrada()).when(entrada).marcarLeida("ana", id);
        var resultado = new NotificacionController(entrada, () -> "ana").marcarLeida(id);
        assertThat(resultado.error().codigo()).isEqualTo(CodigoError.NO_ENCONTRADO);
    }
}
