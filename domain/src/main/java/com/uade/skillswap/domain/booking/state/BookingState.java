package com.uade.skillswap.domain.booking.state;

import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.CancellationOutcome;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.TimeSlot;

// Patrón State: define las acciones posibles sobre una reserva.
public interface BookingState {

    BookingStatus getStatus();

    void accept(Booking booking, UserId actor);

    void reject(Booking booking, UserId actor, String reason);

    void validatePayment(Booking booking, UserId actor);

    void confirmPayment(Booking booking, UserId actor);

    void start(Booking booking, UserId actor);

    void complete(Booking booking, UserId actor);

    UserId ratingRecipientFor(Booking booking, UserId author);

    CancellationOutcome cancel(Booking booking, UserId actor, String reason);

    void proposeReschedule(Booking booking, UserId actor, TimeSlot newSlot);

    void acceptReschedule(Booking booking, UserId actor);

    void rejectReschedule(Booking booking, UserId actor);
}
