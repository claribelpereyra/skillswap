package com.uade.skillswap.domain.booking.observer;

import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.StateTransition;
import com.uade.skillswap.domain.common.UserId;

import java.time.Instant;

// Una transición de la reserva contada a quien la observa; from == null es la solicitud y rescheduled, una reprogramación aceptada.
public record BookingEvent(long bookingId, UserId clientId, UserId providerId, BookingStatus from, BookingStatus to,
                           UserId actor, Instant occurredAt, String reason, boolean rescheduled) {

    public BookingEvent(long bookingId, UserId clientId, UserId providerId, BookingStatus from, BookingStatus to,
                        UserId actor, Instant occurredAt, String reason) {
        this(bookingId, clientId, providerId, from, to, actor, occurredAt, reason, false);
    }

    public static BookingEvent of(long bookingId, UserId clientId, UserId providerId,
                                  StateTransition<BookingStatus> transition) {
        return of(bookingId, clientId, providerId, transition, false);
    }

    public static BookingEvent of(long bookingId, UserId clientId, UserId providerId,
                                  StateTransition<BookingStatus> transition, boolean rescheduled) {
        return new BookingEvent(bookingId, clientId, providerId, transition.from(), transition.to(),
                transition.actor(), transition.occurredAt(), transition.reason(), rescheduled);
    }
}
