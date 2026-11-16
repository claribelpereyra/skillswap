package com.uade.skillswap.domain.wallet;

import com.uade.skillswap.domain.common.SkillCoins;

import java.time.Instant;
import java.util.Objects;

public final class BookingPaymentService {

    private final RefundStrategy refundStrategy;

    public BookingPaymentService(RefundStrategy refundStrategy) {
        this.refundStrategy = Objects.requireNonNull(refundStrategy, "The refund strategy is required");
    }

    public void pay(long bookingId, Wallet clientWallet, Wallet providerWallet, SkillCoins amount, Instant paidAt) {
        Objects.requireNonNull(clientWallet, "The client wallet is required")
                .payBookingTo(providerWallet, amount, bookingId, paidAt);
    }

    public SkillCoins refund(
            long bookingId,
            Wallet clientWallet,
            Wallet providerWallet,
            Instant bookingStartsAt,
            Instant cancelledAt) {
        Objects.requireNonNull(providerWallet, "The provider wallet is required");
        SkillCoins paidAmount = providerWallet.paymentAmountFor(bookingId);
        SkillCoins refundAmount = refundStrategy.refundAmount(paidAmount, bookingStartsAt, cancelledAt);
        providerWallet.refundBookingTo(clientWallet, refundAmount, bookingId, cancelledAt);
        return refundAmount;
    }
}
