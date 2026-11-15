package com.uade.skillswap.application.notificaciones;

import com.uade.skillswap.application.usecases.ProcesarEventoNotificableCasoDeUso;
import com.uade.skillswap.application.ports.in.PreferenciaNotificacionEntrada;
import com.uade.skillswap.application.ports.out.*;
import com.uade.skillswap.domain.notificaciones.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcesarEventoTest {
    @Test void registraFalloDeUnCanalYContinuaConLaOtraParte() {
        var preferencias = mock(PreferenciaNotificacionEntrada.class);
        var repo = mock(RepositorioNotificaciones.class);
        var email = mock(CanalNotificacion.class);
        var push = mock(CanalNotificacion.class);
        var interna = mock(CanalNotificacion.class);
        when(email.tipo()).thenReturn(CanalPreferido.EMAIL);
        when(push.tipo()).thenReturn(CanalPreferido.PUSH);
        when(interna.tipo()).thenReturn(CanalPreferido.INTERNA);
        when(preferencias.consultar("cliente")).thenReturn(new PreferenciaNotificacion("cliente", CanalPreferido.EMAIL));
        when(preferencias.consultar("proveedor")).thenReturn(new PreferenciaNotificacion("proveedor", CanalPreferido.PUSH));
        when(repo.registrarSiAusente(any())).thenReturn(true);
        doThrow(new IllegalStateException("No disponible")).when(email).enviar(any());
        var caso = new ProcesarEventoNotificableCasoDeUso(preferencias, repo,
                new FabricaCanalNotificacion(List.of(email, push, interna)));
        var evento = new EventoNotificable(UUID.randomUUID(), Instant.EPOCH, TipoEventoNotificable.PAGO_CONFIRMADO,
                "1", List.of("cliente", "proveedor"), "Pago confirmado");
        assertThatThrownBy(() -> caso.procesar(evento)).isInstanceOf(IllegalStateException.class);
        verify(repo).registrarEntrega(eq("cliente"), any(), eq(EstadoEntrega.FALLIDA));
        verify(repo).registrarEntrega(eq("proveedor"), any(), eq(EstadoEntrega.ENVIADA));
        verify(push).enviar(argThat(aviso -> aviso.destinatario().equals("proveedor")));
    }

    @Test void noEnviaSiElEventoYaFueRegistrado() {
        var preferencias = mock(PreferenciaNotificacionEntrada.class);
        var repo = mock(RepositorioNotificaciones.class);
        var canales = Arrays.stream(CanalPreferido.values()).map(tipo -> {
            var canal = mock(CanalNotificacion.class);
            when(canal.tipo()).thenReturn(tipo);
            return canal;
        }).toList();
        when(preferencias.consultar("ana")).thenReturn(new PreferenciaNotificacion("ana", CanalPreferido.INTERNA));
        var caso = new ProcesarEventoNotificableCasoDeUso(preferencias, repo, new FabricaCanalNotificacion(canales));
        caso.procesar(new EventoNotificable(UUID.randomUUID(), Instant.EPOCH, TipoEventoNotificable.ACEPTACION,
                "1", List.of("ana"), "Aceptada"));
        for (var canal : canales) verify(canal, never()).enviar(any());
    }
}
