package com.uade.skillswap.domain.booking.observer;

// Observer: recibe los eventos ya confirmados; no debería tirar excepciones porque la reserva ya está guardada.
public interface BookingObserver {

    void update(BookingEvent event);
}
