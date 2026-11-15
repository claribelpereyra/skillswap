package com.uade.skillswap.application.dto.wallet;

import com.uade.skillswap.domain.wallet.Wallet;

import java.util.List;
import java.util.Objects;

public record WalletView(String ownerId, long balance, List<WalletMovementView> movements) {

    public WalletView {
        Objects.requireNonNull(ownerId, "The wallet owner is required");
        movements = List.copyOf(movements);
    }

    public static WalletView from(Wallet wallet) {
        return new WalletView(wallet.ownerId().value(), wallet.balance().amount(),
                wallet.movements().stream().map(WalletMovementView::from).toList());
    }
}
