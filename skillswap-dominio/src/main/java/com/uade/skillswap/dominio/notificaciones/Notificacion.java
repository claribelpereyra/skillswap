package com.uade.skillswap.dominio.notificaciones;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Notificacion(UUID id, Aviso aviso, CanalPreferido canal,
                           EstadoEntrega entrega, Instant leidaEn) {
    public Notificacion {
        Objects.requireNonNull(id);
        Objects.requireNonNull(aviso);
        Objects.requireNonNull(canal);
        Objects.requireNonNull(entrega);
    }

    public boolean leida() { return leidaEn != null; }
}
