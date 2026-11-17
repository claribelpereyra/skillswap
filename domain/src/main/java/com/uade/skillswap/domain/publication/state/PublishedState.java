package com.uade.skillswap.domain.publication.state;

import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.Publication;
import com.uade.skillswap.domain.publication.PublicationStatus;

import java.time.Instant;

public class PublishedState extends PublicationStateBase {

    @Override
    public PublicationStatus getStatus() {
        return PublicationStatus.PUBLISHED;
    }

    @Override
    public void pause(Publication publication, UserId actor, Instant now) {
        publication.changeState(new PausedState(), actor, now, "Paused by the author");
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

    @Override
    public boolean acceptsRequests() {
        return true;
    }

    @Override
    public boolean isPublic() {
        return true;
    }

    @Override
    public boolean allowsReservations() {
        return true;
    }
}
