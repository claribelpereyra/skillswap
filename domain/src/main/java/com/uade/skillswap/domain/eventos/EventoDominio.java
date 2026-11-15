package com.uade.skillswap.domain.eventos;

import java.time.Instant;
import java.util.UUID;

/** Hecho inmutable de negocio; se publica después de confirmar la operación. */
public interface EventoDominio {
    UUID id();
    Instant ocurridoEn();
}
