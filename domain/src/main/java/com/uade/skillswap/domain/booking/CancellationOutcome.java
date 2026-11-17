package com.uade.skillswap.domain.booking;

// Lo que la cancelación deja pendiente según el estado desde el que se canceló.
public record CancellationOutcome(boolean refundable, boolean releasesSlot) {
}
