package com.uade.skillswap.infrastructure.adapters;

import com.uade.skillswap.application.ports.out.wallet.SkillCoinGateway;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;

import java.util.Objects;

public final class SimulatedSkillCoinGateway implements SkillCoinGateway {

    @Override
    public boolean authorizeCredit(UserId userId, SkillCoins amount) {
        Objects.requireNonNull(userId, "The wallet owner is required");
        Objects.requireNonNull(amount, "The credit amount is required");
        return amount.amount() > 0;
    }
}
