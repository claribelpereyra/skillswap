package com.uade.skillswap.domain.wallet;

import com.uade.skillswap.domain.common.SkillCoins;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefundStrategyTest {

    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");

    @Test
    void politicaEscalonadaAplicaUmbralesYRedondeaLaMitadHaciaAbajo() {
        RefundStrategy strategy = new TieredRefundStrategy(
                new RefundPolicy(Duration.ofHours(48), Duration.ofHours(24)));
        SkillCoins amount = new SkillCoins(35);
        Instant startsAt = NOW.plus(Duration.ofHours(48));

        assertThat(strategy.refundAmount(amount, startsAt, NOW)).isEqualTo(amount);
        assertThat(strategy.refundAmount(amount, startsAt, NOW.plus(Duration.ofHours(24))))
                .isEqualTo(new SkillCoins(17));
        assertThat(strategy.refundAmount(amount, startsAt, NOW.plus(Duration.ofHours(25))))
                .isEqualTo(SkillCoins.ZERO);
    }

    @Test
    void estrategiaDeReintegroTotalDevuelveElImportePagado() {
        SkillCoins amount = new SkillCoins(35);

        assertThat(new FullRefundStrategy().refundAmount(
                amount, NOW.plusSeconds(60), NOW)).isEqualTo(amount);
    }

    @Test
    void politicaRechazaUmbralesNegativosODesordenados() {
        assertThatThrownBy(() -> new RefundPolicy(Duration.ofHours(24), Duration.ofHours(48)))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refund notices must be non-negative and full notice must be longer");
    }
}
