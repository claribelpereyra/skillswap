package com.uade.skillswap.domain.publication.state;

import com.uade.skillswap.domain.publication.PublicationStatus;

// Reconstruye el objeto de estado a partir del estado guardado (por ejemplo, al leer de la base).
public final class PublicationStateFactory {

    private PublicationStateFactory() {
    }

    public static PublicationState fromStatus(PublicationStatus status) {
        return switch (status) {
            case DRAFT -> new DraftState();
            case PUBLISHED -> new PublishedState();
            case PAUSED -> new PausedState();
            case CLOSED -> new ClosedState();
            case CANCELLED -> new CancelledState();
        };
    }
}
