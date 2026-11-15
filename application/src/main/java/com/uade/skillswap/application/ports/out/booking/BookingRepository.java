package com.uade.skillswap.application.ports.out.booking;

import com.uade.skillswap.domain.booking.Booking;

import java.util.Optional;

public interface BookingRepository {

    Optional<Booking> findById(long bookingId);

    void save(long bookingId, Booking booking);
}
