package com.uade.skillswap.domain.publication.state;

import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.Publication;
import com.uade.skillswap.domain.publication.PublicationStatus;

import java.time.Instant;

public class DraftState extends PublicationStateBase {

    @Override
    public PublicationStatus getStatus() {
        return PublicationStatus.DRAFT;
    }

    @Override
    public void publish(Publication publication, UserId actor, Instant now) {
        publication.checkCompleteForPublishing();
        publication.changeState(new PublishedState(), actor, now, "Published by the author");
    }

    @Override
    public void cancel(Publication publication, UserId actor, String reason, Instant now) {
        publication.requireReason(reason, "cancel");
        publication.changeState(new CancelledState(), actor, now, reason);
    }

    @Override
    public void checkEditable() {
        // En borrador se puede editar todo.
    }
}
