package com.uade.skillswap.dominio.notificaciones;

import com.uade.skillswap.dominio.eventos.EventoDominio;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Datos de un hecho confirmado. El emisor determina las partes autorizadas. */
public record EventoNotificable(UUID id, Instant ocurridoEn, TipoEventoNotificable tipo,
                               String referencia, List<String> destinatarios, String detalle)
        implements EventoDominio {
    public EventoNotificable {
        Objects.requireNonNull(id);
        Objects.requireNonNull(ocurridoEn);
        Objects.requireNonNull(tipo);
        if (referencia == null || referencia.isBlank()) throw new IllegalArgumentException("Falta referencia");
        if (detalle == null || detalle.isBlank()) throw new IllegalArgumentException("Falta detalle");
        destinatarios = List.copyOf(destinatarios);
        if (destinatarios.isEmpty() || destinatarios.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("Faltan destinatarios válidos");
        }
        if (destinatarios.stream().distinct().count() != destinatarios.size()) {
            throw new IllegalArgumentException("Destinatarios repetidos");
        }
    }
}
