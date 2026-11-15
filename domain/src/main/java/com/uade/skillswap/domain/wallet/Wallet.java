package com.uade.skillswap.domain.wallet;

import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Wallet {

    private final UserId ownerId;
    private final List<CreditMovement> movements = new ArrayList<>();
    private SkillCoins balance = SkillCoins.ZERO;

    private Wallet(UserId ownerId) {
        this.ownerId = Objects.requireNonNull(ownerId, "The wallet owner is required");
    }

    public static Wallet open(UserId ownerId) {
        return new Wallet(ownerId);
    }

    public void topUp(SkillCoins amount, Instant occurredAt) {
        apply(CreditMovementType.TOP_UP, amount, occurredAt, null);
    }

    public void debit(SkillCoins amount, long bookingId, Instant occurredAt) {
        apply(CreditMovementType.DEBIT, amount, occurredAt, bookingId);
    }

    public void credit(SkillCoins amount, long bookingId, Instant occurredAt) {
        apply(CreditMovementType.CREDIT, amount, occurredAt, bookingId);
    }

    public void refund(SkillCoins amount, long bookingId, Instant occurredAt) {
        apply(CreditMovementType.REFUND, amount, occurredAt, bookingId);
    }

    private void apply(CreditMovementType type, SkillCoins amount, Instant occurredAt, Long bookingId) {
        CreditMovement movement = new CreditMovement(type, amount, occurredAt, bookingId);
        if (!movements.isEmpty()
                && occurredAt.isBefore(movements.get(movements.size() - 1).occurredAt())) {
            throw new IllegalArgumentException("A movement cannot be dated before the previous movement");
        }
        SkillCoins nextBalance = switch (type) {
            case TOP_UP, CREDIT, REFUND -> balance.add(amount);
            case DEBIT -> balance.subtract(amount);
        };
        balance = nextBalance;
        movements.add(movement);
    }

    public UserId ownerId() {
        return ownerId;
    }

    public SkillCoins balance() {
        return balance;
    }

    public List<CreditMovement> movements() {
        return List.copyOf(movements);
    }
}
