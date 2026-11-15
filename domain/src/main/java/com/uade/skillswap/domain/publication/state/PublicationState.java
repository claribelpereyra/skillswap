package com.uade.skillswap.domain.publication.state;

import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.Publication;
import com.uade.skillswap.domain.publication.PublicationStatus;

import java.time.Instant;

// Patrón State: define las acciones posibles sobre una publicación.
public interface PublicationState {

    PublicationStatus getStatus();

    void publish(Publication publication, UserId actor, Instant now);

    void pause(Publication publication, UserId actor, Instant now);

    void resume(Publication publication, UserId actor, Instant now);

    void close(Publication publication, UserId actor, Instant now);

    void cancel(Publication publication, UserId actor, String reason, Instant now);

    void checkEditable();

    boolean acceptsRequests();

    boolean isPublic();

    boolean allowsReservations();
}
