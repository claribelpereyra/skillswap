package com.uade.skillswap.application.usecases.booking;

import com.uade.skillswap.application.ports.out.BookablePublication;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.pricing.FixedPromotion;
import com.uade.skillswap.domain.pricing.PercentagePromotion;
import com.uade.skillswap.domain.pricing.Price;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;

class PromotionalPricingPolicyTest {

    private static final UserId CLIENT = new UserId("client");
    private static final UserId PROMOTED_PROVIDER = new UserId("provider");
    private static final UserId OTHER_PROVIDER = new UserId("other");

    private static BookablePublication publicationBy(UserId author, long price) {
        return new BookablePublication(1L, author, true, new SkillCoins(price), List.of());
    }

    private static UnaryOperator<Price> percentage(int value) {
        return price -> new PercentagePromotion(price, value);
    }

    private static UnaryOperator<Price> fixed(long value) {
        return price -> new FixedPromotion(price, new SkillCoins(value));
    }

    @Test
    void appliesTheProviderPromotionsInTheConfiguredOrder() {
        var policy = new PromotionalPricingPolicy(Map.of(PROMOTED_PROVIDER, List.of(percentage(10), fixed(5))));

        assertThat(policy.priceFor(CLIENT, publicationBy(PROMOTED_PROVIDER, 100))).isEqualTo(new SkillCoins(85));
    }

    @Test
    void theOrderOfThePromotionsChangesTheResult() {
        var percentageFirst = new PromotionalPricingPolicy(Map.of(PROMOTED_PROVIDER, List.of(percentage(50), fixed(20))));
        var fixedFirst = new PromotionalPricingPolicy(Map.of(PROMOTED_PROVIDER, List.of(fixed(20), percentage(50))));

        assertThat(percentageFirst.priceFor(CLIENT, publicationBy(PROMOTED_PROVIDER, 100))).isEqualTo(new SkillCoins(30));
        assertThat(fixedFirst.priceFor(CLIENT, publicationBy(PROMOTED_PROVIDER, 100))).isEqualTo(new SkillCoins(40));
    }

    @Test
    void accumulatedPromotionsNeverProduceANegativePrice() {
        var policy = new PromotionalPricingPolicy(
                Map.of(PROMOTED_PROVIDER, List.of(fixed(80), percentage(50), fixed(100))));

        assertThat(policy.priceFor(CLIENT, publicationBy(PROMOTED_PROVIDER, 100))).isEqualTo(SkillCoins.ZERO);
    }

    @Test
    void publicationsOfOtherProvidersKeepTheirBasePrice() {
        var policy = new PromotionalPricingPolicy(Map.of(PROMOTED_PROVIDER, List.of(percentage(10))));

        assertThat(policy.priceFor(CLIENT, publicationBy(OTHER_PROVIDER, 100))).isEqualTo(new SkillCoins(100));
        assertThat(new PromotionalPricingPolicy(Map.of()).priceFor(CLIENT, publicationBy(PROMOTED_PROVIDER, 100)))
                .isEqualTo(new SkillCoins(100));
    }
}
