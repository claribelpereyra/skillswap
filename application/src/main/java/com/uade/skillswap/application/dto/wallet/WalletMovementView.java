package com.uade.skillswap.application.dto.wallet;

import com.uade.skillswap.domain.wallet.CreditMovement;

import java.time.Instant;

public record WalletMovementView(String type, long amount, Instant occurredAt, Long bookingId) {

    public static WalletMovementView from(CreditMovement movement) {
        return new WalletMovementView(movement.type().name(), movement.amount().amount(), movement.occurredAt(),
                movement.bookingId());
    }
}
