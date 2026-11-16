package com.uade.skillswap.domain.notificaciones;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Aviso(UUID eventoId, Instant ocurridoEn, TipoEventoNotificable tipo,
                    String referencia, String destinatario, String detalle) {
    public Aviso {
        Objects.requireNonNull(eventoId);
        Objects.requireNonNull(ocurridoEn);
        Objects.requireNonNull(tipo);
        if (referencia == null || referencia.isBlank() || destinatario == null || destinatario.isBlank()
                || detalle == null || detalle.isBlank()) throw new IllegalArgumentException("Aviso incompleto");
    }
}
