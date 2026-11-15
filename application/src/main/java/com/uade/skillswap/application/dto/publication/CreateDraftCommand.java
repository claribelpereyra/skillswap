package com.uade.skillswap.application.dto.publication;

import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.PublicationDetails;
import com.uade.skillswap.domain.publication.PublicationType;

import java.util.List;
import java.util.Objects;

public record CreateDraftCommand(UserId authorId, PublicationType type, PublicationDetails details,
                                 List<SlotInput> slots) {

    public CreateDraftCommand {
        Objects.requireNonNull(authorId, "The author is required");
        Objects.requireNonNull(type, "The publication type is required");
        Objects.requireNonNull(details, "The publication details are required");
        slots = slots == null ? List.of() : List.copyOf(slots);
    }
}
