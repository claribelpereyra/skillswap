package com.uade.skillswap.application.dto.booking;

import java.time.Instant;
import java.util.Objects;

public record BookingReceipt(long bookingId, String clientId, String providerId, long amount, Instant paidAt,
                             Instant completedAt) {

    public BookingReceipt {
        if (bookingId <= 0) {
            throw new IllegalArgumentException("A booking is required for a receipt");
        }
        Objects.requireNonNull(clientId, "The client is required");
        Objects.requireNonNull(providerId, "The provider is required");
        Objects.requireNonNull(paidAt, "The payment date is required");
        Objects.requireNonNull(completedAt, "The completion date is required");
        if (amount <= 0) {
            throw new IllegalArgumentException("The payment amount must be positive");
        }
    }
}
