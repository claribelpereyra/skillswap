package com.uade.skillswap.domain.notificaciones;

import java.util.Objects;

public record PreferenciaNotificacion(String userId, CanalPreferido canal) {
    public PreferenciaNotificacion {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("Falta userId");
        Objects.requireNonNull(canal, "Falta canal");
    }
}
