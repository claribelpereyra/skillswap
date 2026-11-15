package com.uade.skillswap.domain.booking.state;

import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.UserId;

import java.time.Instant;

public class ConfirmedState extends BookingStateBase {

    @Override
    public BookingStatus getStatus() {
        return BookingStatus.CONFIRMED;
    }

    @Override
    public void start(Booking booking, UserId actor, Instant occurredAt) {
        booking.requireProvider(actor, "start");
        booking.changeState(new InProgressState(), actor, null, occurredAt);
    }

    @Override
    public void cancel(Booking booking, UserId actor, String reason, Instant occurredAt) {
        booking.prepareCancellation(actor, reason);
        booking.changeState(new CancelledState(), actor, reason, occurredAt);
    }
}
