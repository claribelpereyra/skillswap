package com.uade.skillswap.application.ports.in.publication;

import java.time.Instant;

// Franja tal como la carga el autor desde la interfaz.
public record SlotInput(Instant startsAt, Instant endsAt) {
}
