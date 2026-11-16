package com.uade.skillswap.aplicacion.eventos;

import com.uade.skillswap.aplicacion.puertos.entrada.ProcesarEventoNotificableEntrada;
import com.uade.skillswap.dominio.notificaciones.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class SuscriptorNotificacionesTest {
    @ParameterizedTest @EnumSource(TipoEventoNotificable.class)
    void derivaLosSeisEventosPorObserver(TipoEventoNotificable tipo) {
        var procesador = mock(ProcesarEventoNotificableEntrada.class);
        var bus = new PublicadorEventosEnMemoria();
        bus.suscribir(new SuscriptorNotificaciones(procesador));
        var evento = new EventoNotificable(UUID.randomUUID(), Instant.EPOCH, tipo,
                "contratacion-1", List.of("cliente", "proveedor"), "Hecho confirmado");
        bus.publicar(evento);
        verify(procesador).procesar(evento);
    }

    @Test void copiaDestinatariosYRechazaDuplicados() {
        var partes = new ArrayList<>(List.of("cliente"));
        var evento = new EventoNotificable(UUID.randomUUID(), Instant.EPOCH,
                TipoEventoNotificable.ACEPTACION, "1", partes, "Aceptada");
        partes.clear();
        assertThat(evento.destinatarios()).containsExactly("cliente");
        assertThatThrownBy(() -> new EventoNotificable(UUID.randomUUID(), Instant.EPOCH,
                TipoEventoNotificable.ACEPTACION, "1", List.of("a", "a"), "Aceptada"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
