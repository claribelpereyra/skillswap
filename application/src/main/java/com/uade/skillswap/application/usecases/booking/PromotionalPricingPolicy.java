package com.uade.skillswap.application.usecases.booking;

import com.uade.skillswap.application.ports.out.BookablePublication;
import com.uade.skillswap.application.ports.out.BookingPricingPolicy;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.pricing.BasePrice;
import com.uade.skillswap.domain.pricing.Price;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

// Cliente del Decorator: envuelve el precio base con las promociones del proveedor, en el orden configurado.
public final class PromotionalPricingPolicy implements BookingPricingPolicy {

    private final Map<UserId, List<UnaryOperator<Price>>> promotionsByProvider;

    public PromotionalPricingPolicy(Map<UserId, List<UnaryOperator<Price>>> promotionsByProvider) {
        Objects.requireNonNull(promotionsByProvider, "The promotions by provider are required");
        Map<UserId, List<UnaryOperator<Price>>> copy = new HashMap<>();
        promotionsByProvider.forEach((provider, promotions) -> copy.put(provider, List.copyOf(promotions)));
        this.promotionsByProvider = Map.copyOf(copy);
    }

    @Override
    public SkillCoins priceFor(UserId clientId, BookablePublication publication) {
        Price price = new BasePrice(publication.price());
        for (UnaryOperator<Price> promotion : promotionsByProvider.getOrDefault(publication.authorId(), List.of())) {
            price = promotion.apply(price);
        }
        return price.total();
    }
}
