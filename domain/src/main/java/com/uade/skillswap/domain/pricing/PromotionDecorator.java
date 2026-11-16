package com.uade.skillswap.domain.pricing;

import com.uade.skillswap.domain.common.SkillCoins;

import java.util.Objects;

public abstract class PromotionDecorator implements Price {

    private final Price decoratedPrice;

    protected PromotionDecorator(Price decoratedPrice) {
        this.decoratedPrice = Objects.requireNonNull(decoratedPrice, "The decorated price is required");
    }

    @Override
    public final SkillCoins total() {
        long currentAmount = decoratedPrice.total().amount();
        long discountAmount = discountFor(currentAmount);
        return new SkillCoins(currentAmount - Math.min(currentAmount, discountAmount));
    }

    protected abstract long discountFor(long currentAmount);
}
