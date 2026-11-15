package com.uade.skillswap.application.ports.in.booking;

import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;

import java.time.Instant;
import java.util.Objects;

public record BookingReceipt(
        long bookingId,
        UserId clientId,
        UserId providerId,
        SkillCoins amount,
        Instant paidAt,
        Instant completedAt) {

    public BookingReceipt {
        if (bookingId <= 0) {
            throw new IllegalArgumentException("A booking is required for a receipt");
        }
        Objects.requireNonNull(clientId, "The client is required");
        Objects.requireNonNull(providerId, "The provider is required");
        Objects.requireNonNull(amount, "The payment amount is required");
        Objects.requireNonNull(paidAt, "The payment date is required");
        Objects.requireNonNull(completedAt, "The completion date is required");
    }
}
