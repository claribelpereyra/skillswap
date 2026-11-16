package com.uade.skillswap.application.ports.in.wallet;

import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;

import java.util.Objects;

public record LoadWalletCommand(UserId userId, SkillCoins amount) {

    public LoadWalletCommand {
        Objects.requireNonNull(userId, "The wallet owner is required");
        Objects.requireNonNull(amount, "The credit amount is required");
        if (amount.amount() <= 0) {
            throw new IllegalArgumentException("The credit amount must be positive");
        }
    }
}
