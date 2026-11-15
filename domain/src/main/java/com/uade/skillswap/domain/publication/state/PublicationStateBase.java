package com.uade.skillswap.domain.publication.state;

import com.uade.skillswap.domain.common.InvalidTransitionException;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.Publication;

import java.time.Instant;

// Cada estado concreto sobrescribe solo las acciones permitidas; el resto se rechaza.
public abstract class PublicationStateBase implements PublicationState {

    @Override
    public void publish(Publication publication, UserId actor, Instant now) {
        throw invalid("publish");
    }

    @Override
    public void pause(Publication publication, UserId actor, Instant now) {
        throw invalid("pause");
    }

    @Override
    public void resume(Publication publication, UserId actor, Instant now) {
        throw invalid("resume");
    }

    @Override
    public void close(Publication publication, UserId actor, Instant now) {
        throw invalid("close");
    }

    @Override
    public void cancel(Publication publication, UserId actor, String reason, Instant now) {
        throw invalid("cancel");
    }

    @Override
    public void checkEditable() {
        throw invalid("edit");
    }

    @Override
    public boolean acceptsRequests() {
        return false;
    }

    @Override
    public boolean isPublic() {
        return false;
    }

    @Override
    public boolean allowsReservations() {
        return false;
    }

    private InvalidTransitionException invalid(String action) {
        return new InvalidTransitionException(getStatus(), action);
    }
}
