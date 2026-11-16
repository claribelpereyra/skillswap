package com.uade.skillswap.domain.booking.observer;

public interface BookingSubject {

    void addObserver(BookingObserver observer);

    void removeObserver(BookingObserver observer);

    void notifyObservers(BookingEvent event);
}
