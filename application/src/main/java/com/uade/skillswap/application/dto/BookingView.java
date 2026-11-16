package com.uade.skillswap.application.dto;

import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.BookingStatus;

import java.time.Instant;

public record BookingView(Long id, long publicationId, String clientId, String providerId, long slotId,
                          Instant slotStartsAt, Instant slotEndsAt, BookingStatus status, long price,
                          String message, String goal, Long proposedSlotId, String rescheduleProposedBy) {

    public static BookingView from(Booking booking) {
        var proposal = booking.getPendingReschedule();
        return new BookingView(
                booking.getId(),
                booking.getPublicationId(),
                booking.getClientId().value(),
                booking.getProviderId().value(),
                booking.getSlot().getId(),
                booking.getSlot().getStartsAt(),
                booking.getSlot().getEndsAt(),
                booking.getStatus(),
                booking.getPrice().amount(),
                booking.getMessage(),
                booking.getGoal(),
                proposal.map(p -> p.newSlot().getId()).orElse(null),
                proposal.map(p -> p.proposedBy().value()).orElse(null));
    }
}
