package com.uade.skillswap.domain.wallet;

import com.uade.skillswap.domain.common.SkillCoins;

import java.time.Instant;
import java.util.Objects;

public final class FullRefundStrategy implements RefundStrategy {

    @Override
    public SkillCoins refundAmount(SkillCoins paidAmount, Instant bookingStartsAt, Instant cancelledAt) {
        Objects.requireNonNull(paidAmount, "The payment amount is required");
        Objects.requireNonNull(bookingStartsAt, "The booking start is required");
        Objects.requireNonNull(cancelledAt, "The cancellation date is required");
        return paidAmount;
    }
}
