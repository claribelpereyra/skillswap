package com.uade.skillswap.domain.common;

public record SkillCoins(long amount) {

    public static final SkillCoins ZERO = new SkillCoins(0);

    public SkillCoins {
        if (amount < 0) {
            throw new IllegalArgumentException("A SkillCoins amount cannot be negative: " + amount);
        }
    }

    public SkillCoins add(SkillCoins other) {
        return new SkillCoins(Math.addExact(amount, other.amount));
    }

    public SkillCoins subtract(SkillCoins other) {
        if (other.amount > amount) {
            throw new IllegalArgumentException("Insufficient balance: " + amount + " < " + other.amount);
        }
        return new SkillCoins(amount - other.amount);
    }

    public boolean isEnoughFor(SkillCoins price) {
        return amount >= price.amount;
    }
}
