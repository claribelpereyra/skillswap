package com.uade.skillswap.application.ports.in.wallet;

import com.uade.skillswap.domain.common.UserId;

import java.util.Objects;

public record CancelBookingCommand(UserId actorId, long bookingId, String reason) {

    public CancelBookingCommand {
        Objects.requireNonNull(actorId, "The cancelling user is required");
        if (bookingId <= 0) {
            throw new IllegalArgumentException("A booking is required");
        }
    }
}
