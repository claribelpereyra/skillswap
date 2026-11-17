package com.uade.skillswap.domain.booking.state;

import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.CancellationOutcome;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.TimeSlot;

public class ConfirmedState extends BookingStateBase {

    @Override
    public BookingStatus getStatus() {
        return BookingStatus.CONFIRMED;
    }

    @Override
    public void start(Booking booking, UserId actor) {
        booking.requireProvider(actor, "start");
        booking.discardReschedule();
        booking.changeState(new InProgressState(), actor, "Session started by the provider");
    }

    @Override
    public CancellationOutcome cancel(Booking booking, UserId actor, String reason) {
        booking.prepareCancellation(actor, reason);
        booking.changeState(new CancelledState(), actor, reason);
        return new CancellationOutcome(true, true);
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
