package com.uade.skillswap.application.ports.out;

import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;

@FunctionalInterface
public interface BookingPricingPolicy {

    SkillCoins priceFor(UserId clientId, BookablePublication publication);

    static BookingPricingPolicy basePrice() {
        return (clientId, publication) -> publication.price();
    }
}
