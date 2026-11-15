package com.uade.skillswap.application.dto.booking;

import com.uade.skillswap.domain.common.UserId;

import java.util.Objects;

public record GetBookingReceiptCommand(UserId actorId, long bookingId) {

    public GetBookingReceiptCommand {
        Objects.requireNonNull(actorId, "The requesting user is required");
        if (bookingId <= 0) {
            throw new IllegalArgumentException("A booking is required");
        }
    }
}
