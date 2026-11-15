package com.uade.skillswap.application.ports.in.wallet;

import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.wallet.CreditMovement;

import java.util.List;
import java.util.Objects;

public record WalletView(UserId ownerId, SkillCoins balance, List<CreditMovement> movements) {

    public WalletView {
        Objects.requireNonNull(ownerId, "The wallet owner is required");
        Objects.requireNonNull(balance, "The wallet balance is required");
        movements = List.copyOf(movements);
    }
}
