package com.uade.skillswap.aplicacion.eventos;

import com.uade.skillswap.dominio.eventos.EventoDominio;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PublicadorEventosTest {
    private record Hecho(UUID id, Instant ocurridoEn) implements EventoDominio {}

    @Test void filtraEvitaRegistroDuplicadoYPermiteDesuscribir() {
        var bus = new PublicadorEventosEnMemoria();
        var receptor = mock(SuscriptorEvento.class);
        var ignorado = mock(SuscriptorEvento.class);
        var evento = new Hecho(UUID.randomUUID(), Instant.EPOCH);
        when(receptor.acepta(evento)).thenReturn(true);
        bus.suscribir(receptor);
        bus.suscribir(receptor);
        bus.suscribir(ignorado);
        bus.publicar(evento);
        verify(receptor).recibir(evento);
        verify(ignorado, never()).recibir(any());
        bus.desuscribir(receptor);
        bus.publicar(evento);
        verify(receptor, times(1)).recibir(evento);
    }

    @Test void unFalloNoImpideEntregarALosDemas() {
        var bus = new PublicadorEventosEnMemoria();
        var roto = mock(SuscriptorEvento.class);
        var sano = mock(SuscriptorEvento.class);
        when(roto.acepta(any())).thenReturn(true);
        when(sano.acepta(any())).thenReturn(true);
        doThrow(new IllegalStateException("sin conexión")).when(roto).recibir(any());
        bus.suscribir(roto);
        bus.suscribir(sano);
        var evento = new Hecho(UUID.randomUUID(), Instant.EPOCH);
        assertThatThrownBy(() -> bus.publicar(evento)).hasSuppressedException(new IllegalStateException("sin conexión"));
        verify(sano).recibir(evento);
    }
}
