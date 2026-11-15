package com.uade.skillswap.domain.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillCoinsTest {

    @Test
    void addsAndSubtractsAmounts() {
        SkillCoins balance = new SkillCoins(100);

        assertThat(balance.add(new SkillCoins(50))).isEqualTo(new SkillCoins(150));
        assertThat(balance.subtract(new SkillCoins(30))).isEqualTo(new SkillCoins(70));
    }

    @Test
    void doesNotAllowNegativeAmounts() {
        assertThatThrownBy(() -> new SkillCoins(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void doesNotAllowSubtractingMoreThanTheBalance() {
        SkillCoins balance = new SkillCoins(10);

        assertThatThrownBy(() -> balance.subtract(new SkillCoins(11)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance");
        assertThat(balance.isEnoughFor(new SkillCoins(11))).isFalse();
    }
}
