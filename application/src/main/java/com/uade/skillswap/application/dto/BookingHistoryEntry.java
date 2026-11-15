package com.uade.skillswap.application.dto;

import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.StateTransition;

import java.time.Instant;

public record BookingHistoryEntry(BookingStatus from, BookingStatus to, String actorId, Instant occurredAt, String reason) {

    public static BookingHistoryEntry from(StateTransition<BookingStatus> transition) {
        return new BookingHistoryEntry(transition.from(), transition.to(), transition.actor().value(),
                transition.occurredAt(), transition.reason());
    }
}
