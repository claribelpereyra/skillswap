package com.uade.skillswap.domain.booking.state;

import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.CancellationOutcome;
import com.uade.skillswap.domain.common.InvalidTransitionException;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.TimeSlot;

// Cada estado concreto sobrescribe solo las acciones permitidas.
public abstract class BookingStateBase implements BookingState {

    @Override
    public void accept(Booking booking, UserId actor) {
        throw invalid("accept");
    }

    @Override
    public void reject(Booking booking, UserId actor, String reason) {
        throw invalid("reject");
    }

    @Override
    public void validatePayment(Booking booking, UserId actor) {
        throw invalid("pay");
    }

    @Override
    public void confirmPayment(Booking booking, UserId actor) {
        throw invalid("pay");
    }

    @Override
    public void start(Booking booking, UserId actor) {
        throw invalid("start");
    }

    @Override
    public void complete(Booking booking, UserId actor) {
        throw invalid("complete");
    }

    @Override
    public UserId ratingRecipientFor(Booking booking, UserId author) {
        throw invalid("rate");
    }

    @Override
    public CancellationOutcome cancel(Booking booking, UserId actor, String reason) {
        throw invalid("cancel");
    }

    @Override
    public void proposeReschedule(Booking booking, UserId actor, TimeSlot newSlot) {
        throw invalid("reschedule");
    }

    @Override
    public void acceptReschedule(Booking booking, UserId actor) {
        throw invalid("accept a reschedule");
    }

    @Override
    public void rejectReschedule(Booking booking, UserId actor) {
        throw invalid("reject a reschedule");
    }

    private InvalidTransitionException invalid(String action) {
        return new InvalidTransitionException(getStatus(), action);
    }
}
