package com.uade.skillswap.infrastructure.persistence.rating;

import com.uade.skillswap.application.ports.out.BookingRepository;
import com.uade.skillswap.application.ports.out.rating.EligibleBookingQuery;
import com.uade.skillswap.domain.booking.BookingStatus;

import java.util.Objects;
import java.util.Optional;

public class JpaEligibleBookingQuery implements EligibleBookingQuery {

    private final BookingRepository bookings;

    public JpaEligibleBookingQuery(BookingRepository bookings) {
        this.bookings = Objects.requireNonNull(bookings, "bookings must not be null");
    }

    @Override
    public Optional<BookingTarget> findBooking(long bookingId) {
        return bookings.findById(bookingId).map(booking -> new BookingTarget(
                booking.getId(),
                booking.getClientId(),
                booking.getProviderId(),
                booking.getStatus() == BookingStatus.COMPLETED));
    }
}
