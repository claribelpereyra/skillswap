package com.uade.skillswap.domain.publication.state;

import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.Publication;
import com.uade.skillswap.domain.publication.PublicationStatus;

import java.time.Instant;

public class PausedState extends PublicationStateBase {

    @Override
    public PublicationStatus getStatus() {
        return PublicationStatus.PAUSED;
    }

    @Override
    public void resume(Publication publication, UserId actor, Instant now) {
        publication.changeState(new PublishedState(), actor, now, "Resumed by the author");
    }

    @Override
    public void close(Publication publication, UserId actor, Instant now) {
        publication.changeState(new ClosedState(), actor, now, "Closed by the author");
    }

    @Override
    public void cancel(Publication publication, UserId actor, String reason, Instant now) {
        publication.requireReason(reason, "cancel");
        publication.changeState(new CancelledState(), actor, now, reason);
    }

    // No recibe solicitudes nuevas, pero se pueden aceptar las pendientes.
    @Override
    public boolean allowsReservations() {
        return true;
    }
}
