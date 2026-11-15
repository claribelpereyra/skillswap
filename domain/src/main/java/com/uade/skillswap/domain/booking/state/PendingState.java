package com.uade.skillswap.domain.booking.state;

import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.UserId;

import java.time.Instant;

public class PendingState extends BookingStateBase {

    @Override
    public BookingStatus getStatus() {
        return BookingStatus.PENDING;
    }

    @Override
    public void accept(Booking booking, UserId actor, Instant occurredAt) {
        booking.requireProvider(actor, "accept");
        booking.changeState(new AcceptedState(), actor, null, occurredAt);
    }

    @Override
    public void reject(Booking booking, UserId actor, String reason, Instant occurredAt) {
        booking.requireProvider(actor, "reject");
        booking.requireReason(reason, "reject");
        booking.changeState(new RejectedState(), actor, reason, occurredAt);
    }
}
