package com.uade.skillswap.domain.wallet;

import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.pricing.BasePrice;
import com.uade.skillswap.domain.pricing.Price;

import java.time.Instant;
import java.util.Objects;
import java.util.function.UnaryOperator;

public final class BookingPaymentService {

    private final RefundStrategy refundStrategy;
    private final UnaryOperator<Price> promotions;

    public BookingPaymentService(RefundStrategy refundStrategy) {
        this(refundStrategy, UnaryOperator.identity());
    }

    public BookingPaymentService(RefundStrategy refundStrategy, UnaryOperator<Price> promotions) {
        this.refundStrategy = Objects.requireNonNull(refundStrategy, "The refund strategy is required");
        this.promotions = Objects.requireNonNull(promotions, "The promotion decorator is required");
    }

    public void pay(long bookingId, Wallet clientWallet, Wallet providerWallet, SkillCoins amount, Instant paidAt) {
        Objects.requireNonNull(clientWallet, "The client wallet is required");
        Objects.requireNonNull(providerWallet, "The provider wallet is required");
        Objects.requireNonNull(amount, "The payment amount is required");
        Objects.requireNonNull(paidAt, "The payment date is required");

        Price price = Objects.requireNonNull(
                promotions.apply(new BasePrice(amount)),
                "The decorated price is required");
        SkillCoins chargedAmount = Objects.requireNonNull(price.total(), "The charged amount is required");
        if (chargedAmount.amount() == 0) {
            throw new BusinessRuleException("The booking payment must remain positive after promotions");
        }
        clientWallet.payBookingTo(providerWallet, chargedAmount, bookingId, paidAt);
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
