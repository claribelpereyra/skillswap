package com.uade.skillswap.infrastructure.adapters;

import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedSkillCoinGatewayTest {

    private final SimulatedSkillCoinGateway gateway = new SimulatedSkillCoinGateway();

    @Test
    void itApprovesPositiveSkillCoinLoadsWithoutConnectingToAnExternalProvider() {
        assertThat(gateway.authorizeCredit(new UserId("ana"), new SkillCoins(50))).isTrue();
        assertThat(gateway.authorizeCredit(new UserId("ana"), SkillCoins.ZERO)).isFalse();
    }
}
