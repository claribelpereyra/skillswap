package com.uade.skillswap.domain.booking.state;

import java.time.Instant;
import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.UserId;

public class PendingState extends BookingStateBase {

    @Override
    public BookingStatus getStatus() {
        return BookingStatus.PENDING;
    }

    @Override
    public void accept(Booking booking, UserId actor, Instant now) {
        booking.requireProvider(actor, "accept");
        booking.changeState(new AcceptedState(), actor, "Accepted by the provider", now);
    }

    @Override
    public void reject(Booking booking, UserId actor, String reason, Instant now) {
        booking.requireProvider(actor, "reject");
        booking.requireReason(reason, "reject");
        booking.changeState(new RejectedState(), actor, reason, now);
    }
}
