package com.uade.skillswap.domain.pricing;

import com.uade.skillswap.domain.common.SkillCoins;

import java.util.Objects;

public final class FixedPromotion extends PromotionDecorator {

    private final SkillCoins discount;

    public FixedPromotion(Price decoratedPrice, SkillCoins discount) {
        super(decoratedPrice);
        this.discount = Objects.requireNonNull(discount, "The fixed discount is required");
    }

    @Override
    protected long discountFor(long currentAmount) {
        return discount.amount();
    }
}
