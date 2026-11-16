package com.uade.skillswap.controllers;

import com.uade.skillswap.controllers.notification.NotificationControllerImpl;

import com.uade.skillswap.application.notificaciones.*;
import com.uade.skillswap.application.ports.in.BandejaNotificacionEntrada;
import com.uade.skillswap.domain.common.UserId;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificacionControllerTest {
    @Test void usaElUsuarioActualEnCadaOperacionYDelega() {
        var entrada = mock(BandejaNotificacionEntrada.class);
        var usuario = new AtomicReference<>(new UserId("ana"));
        var controller = new NotificationControllerImpl(entrada, usuario::get);
        var pagina = new PaginaNotificaciones(List.of(), 0, 10, 0);
        when(entrada.listar("ana", 0, 10)).thenReturn(pagina);
        var result = controller.listar(0, 10).getData();
        assertThat(result.items()).isEmpty();
        assertThat(result.request().size()).isEqualTo(10);
        assertThat(result.totalItems()).isZero();
        usuario.set(new UserId("bea"));
        var id = UUID.randomUUID();
        assertThat(controller.marcarLeida(id).isSuccess()).isTrue();
        verify(entrada).marcarLeida("bea", id);
    }

    @Test void sinSesionOEntradaInvalidaNoLlamaAlCasoDeUso() {
        var entrada = mock(BandejaNotificacionEntrada.class);
        var sinSesion = new NotificationControllerImpl(entrada, () -> null);
        assertThat(sinSesion.listar(0, 10).getErrorType()).isEqualTo(ControllerResult.ErrorType.ACCESS_DENIED);
        assertThat(sinSesion.marcarLeida(UUID.randomUUID()).getErrorType()).isEqualTo(ControllerResult.ErrorType.ACCESS_DENIED);
        var controller = new NotificationControllerImpl(entrada, () -> new UserId("ana"));
        assertThat(controller.listar(-1, 10).getErrorType()).isEqualTo(ControllerResult.ErrorType.INVALID_INPUT);
        assertThat(controller.marcarLeida(null).getErrorType()).isEqualTo(ControllerResult.ErrorType.INVALID_INPUT);
        verifyNoInteractions(entrada);
    }

    @Test void noExponeUnaNotificacionAjena() {
        var entrada = mock(BandejaNotificacionEntrada.class);
        var id = UUID.randomUUID();
        doThrow(new NotificacionNoEncontrada()).when(entrada).marcarLeida("ana", id);
        var resultado = new NotificationControllerImpl(entrada, () -> new UserId("ana")).marcarLeida(id);
        assertThat(resultado.getErrorType()).isEqualTo(ControllerResult.ErrorType.NOT_FOUND);
    }
}
