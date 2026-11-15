package com.uade.skillswap.domain.booking.state;

import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.UserId;

public class AcceptedState extends BookingStateBase {

    @Override
    public BookingStatus getStatus() {
        return BookingStatus.ACCEPTED;
    }

    @Override
    public void confirmPayment(Booking booking, UserId actor) {
        booking.requireClient(actor, "pay");
        booking.changeState(new ConfirmedState(), actor, null);
    }

    @Override
    public void cancel(Booking booking, UserId actor, String reason) {
        booking.prepareCancellation(actor, reason);
        booking.changeState(new CancelledState(), actor, reason);
    }
}
