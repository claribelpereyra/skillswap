package com.uade.skillswap.application.ports.out;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.common.UserId;

import java.util.Optional;

public interface BookingRepository {

    Booking save(Booking booking);

    Optional<Booking> findById(Long id);

    Page<Booking> findByClient(UserId clientId, PageRequest page);

    Page<Booking> findByProvider(UserId providerId, PageRequest page);
}
