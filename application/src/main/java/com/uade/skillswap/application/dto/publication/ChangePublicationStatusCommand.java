package com.uade.skillswap.application.dto.publication;

import com.uade.skillswap.domain.common.UserId;

import java.util.Objects;

// El motivo solo es obligatorio para cancelar; lo valida el dominio.
public record ChangePublicationStatusCommand(UserId actorId, long publicationId, PublicationAction action,
                                             String reason) {

    public ChangePublicationStatusCommand {
        Objects.requireNonNull(actorId, "The actor is required");
        Objects.requireNonNull(action, "The action is required");
    }
}
