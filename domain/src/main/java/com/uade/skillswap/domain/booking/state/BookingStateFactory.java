package com.uade.skillswap.domain.booking.state;

import com.uade.skillswap.domain.booking.BookingStatus;

// Reconstruye el objeto de estado a partir del estado guardado (por ejemplo, al leer de la base).
public final class BookingStateFactory {

    private BookingStateFactory() {
    }

    public static BookingState fromStatus(BookingStatus status) {
        return switch (status) {
            case PENDING -> new PendingState();
            case ACCEPTED -> new AcceptedState();
            case CONFIRMED -> new ConfirmedState();
            case IN_PROGRESS -> new InProgressState();
            case COMPLETED -> new CompletedState();
            case REJECTED -> new RejectedState();
            case CANCELLED -> new CancelledState();
        };
    }
}
