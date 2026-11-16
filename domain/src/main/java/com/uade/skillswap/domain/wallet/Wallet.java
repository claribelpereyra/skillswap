package com.uade.skillswap.domain.wallet;

import com.uade.skillswap.domain.common.BusinessRuleException;
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

    public static Wallet restore(UserId ownerId, List<CreditMovement> movements) {
        Wallet wallet = new Wallet(ownerId);
        Objects.requireNonNull(movements, "The wallet movements are required")
                .forEach(wallet::apply);
        return wallet;
    }

    public void topUp(SkillCoins amount, Instant occurredAt) {
        apply(CreditMovementType.TOP_UP, amount, occurredAt, null);
    }

    public void payBookingTo(Wallet providerWallet, SkillCoins amount, long bookingId, Instant occurredAt) {
        Objects.requireNonNull(providerWallet, "The provider wallet is required");
        Objects.requireNonNull(amount, "The payment amount is required");
        Objects.requireNonNull(occurredAt, "The payment date is required");
        validateDifferentOwners(providerWallet);
        requireBookingId(bookingId);
        if (hasMovement(CreditMovementType.DEBIT, bookingId)
                || providerWallet.hasMovement(CreditMovementType.CREDIT, bookingId)) {
            throw new BusinessRuleException("A payment already exists for booking " + bookingId);
        }

        CreditMovement debit = prepare(CreditMovementType.DEBIT, amount, occurredAt, bookingId);
        CreditMovement credit = providerWallet.prepare(CreditMovementType.CREDIT, amount, occurredAt, bookingId);
        SkillCoins nextPayerBalance = balance.subtract(amount);
        SkillCoins nextProviderBalance = providerWallet.balance.add(amount);

        commit(debit, nextPayerBalance);
        providerWallet.commit(credit, nextProviderBalance);
    }

    public void refundBookingTo(Wallet clientWallet, SkillCoins amount, long bookingId, Instant occurredAt) {
        Objects.requireNonNull(clientWallet, "The client wallet is required");
        Objects.requireNonNull(amount, "The refund amount is required");
        Objects.requireNonNull(occurredAt, "The refund date is required");
        validateDifferentOwners(clientWallet);
        requireBookingId(bookingId);

        SkillCoins paidAmount = movementAmount(CreditMovementType.CREDIT, bookingId);
        SkillCoins clientPayment = clientWallet.movementAmount(CreditMovementType.DEBIT, bookingId);
        if (paidAmount == null || !paidAmount.equals(clientPayment)) {
            throw new BusinessRuleException("No matching payment exists for booking " + bookingId);
        }
        if (hasMovement(CreditMovementType.DEBIT, bookingId)
                || clientWallet.hasMovement(CreditMovementType.REFUND, bookingId)) {
            throw new BusinessRuleException("A refund already exists for booking " + bookingId);
        }
        if (amount.amount() > paidAmount.amount()) {
            throw new BusinessRuleException("A refund cannot exceed the booking payment");
        }
        if (amount.equals(SkillCoins.ZERO)) {
            return;
        }

        CreditMovement debit = prepare(CreditMovementType.DEBIT, amount, occurredAt, bookingId);
        CreditMovement refund = clientWallet.prepare(CreditMovementType.REFUND, amount, occurredAt, bookingId);
        SkillCoins nextProviderBalance = balance.subtract(amount);
        SkillCoins nextClientBalance = clientWallet.balance.add(amount);

        commit(debit, nextProviderBalance);
        clientWallet.commit(refund, nextClientBalance);
    }

    public SkillCoins paymentAmountFor(long bookingId) {
        requireBookingId(bookingId);
        SkillCoins amount = movementAmount(CreditMovementType.CREDIT, bookingId);
        if (amount == null) {
            throw new BusinessRuleException("No payment exists for booking " + bookingId);
        }
        return amount;
    }

    private void apply(CreditMovementType type, SkillCoins amount, Instant occurredAt, Long bookingId) {
        apply(new CreditMovement(type, amount, occurredAt, bookingId));
    }

    private void apply(CreditMovement movement) {
        movement = prepare(movement.type(), movement.amount(), movement.occurredAt(), movement.bookingId());
        SkillCoins nextBalance = switch (movement.type()) {
            case TOP_UP, CREDIT, REFUND -> balance.add(movement.amount());
            case DEBIT -> balance.subtract(movement.amount());
        };
        commit(movement, nextBalance);
    }

    private CreditMovement prepare(CreditMovementType type, SkillCoins amount, Instant occurredAt, Long bookingId) {
        CreditMovement movement = new CreditMovement(type, amount, occurredAt, bookingId);
        if (!movements.isEmpty()
                && occurredAt.isBefore(movements.get(movements.size() - 1).occurredAt())) {
            throw new IllegalStateException("A movement cannot be dated before the previous movement");
        }
        return movement;
    }

    private void commit(CreditMovement movement, SkillCoins nextBalance) {
        balance = nextBalance;
        movements.add(movement);
    }

    private SkillCoins movementAmount(CreditMovementType type, long bookingId) {
        return movements.stream()
                .filter(movement -> movement.type() == type && Objects.equals(movement.bookingId(), bookingId))
                .map(CreditMovement::amount)
                .findFirst()
                .orElse(null);
    }

    private boolean hasMovement(CreditMovementType type, long bookingId) {
        return movementAmount(type, bookingId) != null;
    }

    private void validateDifferentOwners(Wallet otherWallet) {
        if (this == otherWallet || ownerId.equals(otherWallet.ownerId)) {
            throw new BusinessRuleException("A booking payment requires two different wallet owners");
        }
    }

    private static void requireBookingId(long bookingId) {
        if (bookingId <= 0) {
            throw new BusinessRuleException("A booking is required for this movement");
        }
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
