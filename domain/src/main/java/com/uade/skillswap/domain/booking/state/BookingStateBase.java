package com.uade.skillswap.domain.booking.state;

import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.common.InvalidTransitionException;
import com.uade.skillswap.domain.common.UserId;

import java.time.Instant;

// Cada estado concreto sobrescribe solo las acciones permitidas.
public abstract class BookingStateBase implements BookingState {

    @Override
    public void accept(Booking booking, UserId actor, Instant occurredAt) {
        throw invalid("accept");
    }

    @Override
    public void reject(Booking booking, UserId actor, String reason, Instant occurredAt) {
        throw invalid("reject");
    }

    @Override
    public void confirmPayment(Booking booking, UserId actor, Instant occurredAt) {
        throw invalid("pay");
    }

    @Override
    public void start(Booking booking, UserId actor, Instant occurredAt) {
        throw invalid("start");
    }

    @Override
    public void complete(Booking booking, UserId actor, Instant occurredAt) {
        throw invalid("complete");
    }

    @Override
    public void cancel(Booking booking, UserId actor, String reason, Instant occurredAt) {
        throw invalid("cancel");
    }

    private InvalidTransitionException invalid(String action) {
        return new InvalidTransitionException(getStatus(), action);
    }
}
