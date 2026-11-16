package com.uade.skillswap.domain.pricing;

public final class PercentagePromotion extends PromotionDecorator {

    private final int percentage;

    public PercentagePromotion(Price decoratedPrice, int percentage) {
        super(decoratedPrice);
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("A promotion percentage must be between 0 and 100");
        }
        this.percentage = percentage;
    }

    @Override
    protected long discountFor(long currentAmount) {
        return (currentAmount / 100 * percentage) + (currentAmount % 100 * percentage / 100);
    }
}
