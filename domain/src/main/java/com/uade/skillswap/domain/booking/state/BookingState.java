package com.uade.skillswap.domain.booking.state;

import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.UserId;

import java.time.Instant;

// Patrón State: define las acciones posibles sobre una reserva.
public interface BookingState {

    BookingStatus getStatus();

    void accept(Booking booking, UserId actor, Instant occurredAt);

    void reject(Booking booking, UserId actor, String reason, Instant occurredAt);

    void confirmPayment(Booking booking, UserId actor, Instant occurredAt);

    void start(Booking booking, UserId actor, Instant occurredAt);

    void complete(Booking booking, UserId actor, Instant occurredAt);

    void cancel(Booking booking, UserId actor, String reason, Instant occurredAt);
}
