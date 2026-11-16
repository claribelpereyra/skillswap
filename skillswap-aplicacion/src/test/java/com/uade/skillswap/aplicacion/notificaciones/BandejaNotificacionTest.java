package com.uade.skillswap.aplicacion.notificaciones;

import com.uade.skillswap.aplicacion.casosdeuso.BandejaNotificacionCasoDeUso;
import com.uade.skillswap.aplicacion.puertos.salida.RepositorioNotificaciones;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BandejaNotificacionTest {
    @Test void consultaSoloAlActorYValidaPaginaAntesDeAccederAlRepositorio() {
        var repo = mock(RepositorioNotificaciones.class);
        var caso = new BandejaNotificacionCasoDeUso(repo, Clock.systemUTC());
        var pagina = new PaginaNotificaciones(List.of(), 0, 10, 0);
        when(repo.listar("ana", 0, 10)).thenReturn(pagina);
        assertThat(caso.listar("ana", 0, 10)).isEqualTo(pagina);
        assertThatThrownBy(() -> caso.listar("ana", -1, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> caso.listar("ana", 0, 101)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> caso.listar("ana", Integer.MAX_VALUE, 100)).isInstanceOf(IllegalArgumentException.class);
        verify(repo, times(1)).listar(anyString(), anyInt(), anyInt());
    }

    @Test void unaMarcaAjenaONoExistenteNoExponeDatosYLaValidaUsaRelojInyectado() {
        var repo = mock(RepositorioNotificaciones.class);
        var caso = new BandejaNotificacionCasoDeUso(repo, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        var id = UUID.randomUUID();
        when(repo.marcarLeida("ana", id, Instant.EPOCH)).thenReturn(false, true);
        assertThatThrownBy(() -> caso.marcarLeida("ana", id)).isInstanceOf(NotificacionNoEncontrada.class);
        caso.marcarLeida("ana", id);
        verify(repo, times(2)).marcarLeida("ana", id, Instant.EPOCH);
    }
}
