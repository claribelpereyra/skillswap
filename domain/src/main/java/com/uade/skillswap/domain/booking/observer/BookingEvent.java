package com.uade.skillswap.domain.booking.observer;

import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.StateTransition;
import com.uade.skillswap.domain.common.UserId;

import java.time.Instant;

// Una transición de la reserva contada a quien la observa; from == null es la solicitud y from == to la reprogramación.
public record BookingEvent(long bookingId, UserId clientId, UserId providerId, BookingStatus from, BookingStatus to,
                           UserId actor, Instant occurredAt, String reason) {

    public static BookingEvent of(long bookingId, UserId clientId, UserId providerId,
                                  StateTransition<BookingStatus> transition) {
        return new BookingEvent(bookingId, clientId, providerId, transition.from(), transition.to(),
                transition.actor(), transition.occurredAt(), transition.reason());
    }
}
