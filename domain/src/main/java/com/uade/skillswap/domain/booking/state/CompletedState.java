package com.uade.skillswap.domain.booking.state;

import com.uade.skillswap.domain.booking.BookingStatus;

public class CompletedState extends BookingStateBase {

    @Override
    public BookingStatus getStatus() {
        return BookingStatus.COMPLETED;
    }
}
