package com.uade.skillswap.domain.booking;

import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.TimeSlot;

import java.util.Objects;

public record RescheduleProposal(TimeSlot newSlot, UserId proposedBy) {

    public RescheduleProposal {
        Objects.requireNonNull(newSlot, "The new slot is required");
        Objects.requireNonNull(proposedBy, "Who proposed the change is required");
    }
}
