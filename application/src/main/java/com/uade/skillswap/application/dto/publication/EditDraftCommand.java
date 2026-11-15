package com.uade.skillswap.application.dto.publication;

import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.PublicationDetails;

import java.util.List;
import java.util.Objects;

// Reemplaza los datos y las franjas del borrador por los que se envían.
public record EditDraftCommand(UserId actorId, long publicationId, PublicationDetails details,
                               List<SlotInput> slots) {

    public EditDraftCommand {
        Objects.requireNonNull(actorId, "The actor is required");
        Objects.requireNonNull(details, "The publication details are required");
        slots = slots == null ? List.of() : List.copyOf(slots);
    }
}
