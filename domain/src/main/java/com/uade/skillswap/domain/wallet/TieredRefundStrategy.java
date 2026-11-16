package com.uade.skillswap.domain.wallet;

import com.uade.skillswap.domain.common.SkillCoins;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class TieredRefundStrategy implements RefundStrategy {

    private final RefundPolicy policy;

    public TieredRefundStrategy(RefundPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "The refund policy is required");
    }

    @Override
    public SkillCoins refundAmount(SkillCoins paidAmount, Instant bookingStartsAt, Instant cancelledAt) {
        Objects.requireNonNull(paidAmount, "The payment amount is required");
        Objects.requireNonNull(bookingStartsAt, "The booking start is required");
        Objects.requireNonNull(cancelledAt, "The cancellation date is required");

        Duration notice = Duration.between(cancelledAt, bookingStartsAt);
        if (notice.compareTo(policy.fullRefundNotice()) >= 0) {
            return paidAmount;
        }
        if (notice.compareTo(policy.halfRefundNotice()) >= 0) {
            return new SkillCoins(paidAmount.amount() / 2);
        }
        return SkillCoins.ZERO;
    }
}
