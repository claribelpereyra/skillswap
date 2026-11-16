package com.uade.skillswap.domain.pricing;

import com.uade.skillswap.domain.common.SkillCoins;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromotionTest {

    @Test
    void promocionesAcumuladasNoDejanPrecioNegativo() {
        Price price = new FixedPromotion(
                new PercentagePromotion(
                        new FixedPromotion(new BasePrice(new SkillCoins(100)), new SkillCoins(30)),
                        50),
                new SkillCoins(100));

        assertThat(price.total()).isEqualTo(SkillCoins.ZERO);
    }

    @Test
    void percentagePromotionAppliesToTheDecoratedTotalWithoutOverflow() {
        Price price = new PercentagePromotion(new BasePrice(new SkillCoins(Long.MAX_VALUE)), 50);

        assertThat(price.total()).isEqualTo(new SkillCoins(Long.MAX_VALUE / 2 + 1));
    }

    @Test
    void promotionPercentageMustBeBetweenZeroAndOneHundred() {
        assertThatThrownBy(() -> new PercentagePromotion(new BasePrice(new SkillCoins(100)), 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 0 and 100");
    }
}
