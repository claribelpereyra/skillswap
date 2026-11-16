package com.uade.skillswap.domain.wallet;

import com.uade.skillswap.domain.common.SkillCoins;

import java.time.Instant;
import java.util.Objects;

public record CreditMovement(CreditMovementType type, SkillCoins amount, Instant occurredAt, Long bookingId) {

    public CreditMovement {
        Objects.requireNonNull(type, "The movement type is required");
        Objects.requireNonNull(amount, "The movement amount is required");
        Objects.requireNonNull(occurredAt, "The movement date is required");
        if (amount.amount() == 0) {
            throw new IllegalArgumentException("A credit movement amount must be positive");
        }
        if (type == CreditMovementType.TOP_UP) {
            if (bookingId != null) {
                throw new IllegalArgumentException("A top-up cannot be associated with a booking");
            }
        } else if (bookingId == null || bookingId <= 0) {
            throw new IllegalArgumentException("A booking is required for this movement");
        }
    }
}
