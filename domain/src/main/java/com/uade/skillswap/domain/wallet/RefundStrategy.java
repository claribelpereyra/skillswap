package com.uade.skillswap.domain.wallet;

import com.uade.skillswap.domain.common.SkillCoins;

import java.time.Instant;

@FunctionalInterface
public interface RefundStrategy {

    // Strategy: cada política calcula el importe sin acoplarse al flujo de pago.
    SkillCoins refundAmount(SkillCoins paidAmount, Instant bookingStartsAt, Instant cancelledAt);
}
