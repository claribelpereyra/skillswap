package com.uade.skillswap.domain.booking.state;

import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.UserId;

public class CompletedState extends BookingStateBase {

    @Override
    public BookingStatus getStatus() {
        return BookingStatus.COMPLETED;
    }

    @Override
    public UserId ratingRecipientFor(Booking booking, UserId author) {
        booking.requireParticipant(author, "rate");
        return booking.counterpartOf(author);
    }
}
