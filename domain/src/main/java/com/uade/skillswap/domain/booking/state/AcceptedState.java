package com.uade.skillswap.domain.booking.state;

import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.TimeSlot;

public class AcceptedState extends BookingStateBase {

    @Override
    public BookingStatus getStatus() {
        return BookingStatus.ACCEPTED;
    }

    @Override
    public void validatePayment(Booking booking, UserId actor) {
        booking.requireClient(actor, "pay");
    }

    @Override
    public void confirmPayment(Booking booking, UserId actor) {
        validatePayment(booking, actor);
        booking.changeState(new ConfirmedState(), actor, "Payment confirmed by the client");
    }

    @Override
    public void cancel(Booking booking, UserId actor, String reason) {
        booking.prepareCancellation(actor, reason);
        booking.changeState(new CancelledState(), actor, reason);
    }

    @Override
    public void proposeReschedule(Booking booking, UserId actor, TimeSlot newSlot) {
        booking.requireParticipant(actor, "reschedule");
        booking.registerReschedule(actor, newSlot);
    }

    @Override
    public void acceptReschedule(Booking booking, UserId actor) {
        booking.requireParticipant(actor, "accept a reschedule of");
        booking.acceptPendingReschedule(actor);
    }

    @Override
    public void rejectReschedule(Booking booking, UserId actor) {
        booking.requireParticipant(actor, "reject a reschedule of");
        booking.rejectPendingReschedule(actor);
    }
}
