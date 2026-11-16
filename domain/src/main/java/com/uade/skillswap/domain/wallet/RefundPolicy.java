package com.uade.skillswap.domain.wallet;

import java.time.Duration;
import java.util.Objects;

public record RefundPolicy(Duration fullRefundNotice, Duration halfRefundNotice) {

    public RefundPolicy {
        Objects.requireNonNull(fullRefundNotice, "The full-refund notice is required");
        Objects.requireNonNull(halfRefundNotice, "The half-refund notice is required");
        if (fullRefundNotice.isNegative()
                || halfRefundNotice.isNegative()
                || fullRefundNotice.compareTo(halfRefundNotice) < 0) {
            throw new IllegalArgumentException("Refund notices must be non-negative and full notice must be longer");
        }
    }
}
