package com.uade.skillswap.domain.pricing;

import com.uade.skillswap.domain.common.SkillCoins;

import java.util.Objects;

public record BasePrice(SkillCoins total) implements Price {

    public BasePrice {
        Objects.requireNonNull(total, "The base price is required");
    }
}
