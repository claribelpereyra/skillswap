package com.uade.skillswap.application.ports.in.wallet;

import com.uade.skillswap.domain.common.UserId;

import java.util.Objects;

public record PayBookingCommand(UserId actorId, long bookingId) {

    public PayBookingCommand {
        Objects.requireNonNull(actorId, "The paying user is required");
        if (bookingId <= 0) {
            throw new IllegalArgumentException("A booking is required");
        }
    }
}
