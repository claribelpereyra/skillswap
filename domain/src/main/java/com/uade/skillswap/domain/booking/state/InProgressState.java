package com.uade.skillswap.domain.booking.state;

import java.time.Instant;
import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.UserId;

public class InProgressState extends BookingStateBase {

    @Override
    public BookingStatus getStatus() {
        return BookingStatus.IN_PROGRESS;
    }

    @Override
    public void complete(Booking booking, UserId actor, Instant now) {
        booking.requireParticipant(actor, "complete");
        booking.changeState(new CompletedState(), actor, "Session completed", now);
    }
}
