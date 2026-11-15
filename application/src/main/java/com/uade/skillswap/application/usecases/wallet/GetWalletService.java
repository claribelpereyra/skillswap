package com.uade.skillswap.application.usecases.wallet;

import com.uade.skillswap.application.common.AccessDeniedException;
import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.ports.in.wallet.GetWalletCommand;
import com.uade.skillswap.application.ports.in.wallet.GetWalletUseCase;
import com.uade.skillswap.application.ports.in.wallet.WalletView;
import com.uade.skillswap.application.ports.out.wallet.WalletRepository;
import com.uade.skillswap.domain.wallet.Wallet;

import java.util.Objects;

public final class GetWalletService implements GetWalletUseCase {

    private final WalletRepository wallets;

    public GetWalletService(WalletRepository wallets) {
        this.wallets = Objects.requireNonNull(wallets, "The wallet repository is required");
    }

    @Override
    public WalletView getWallet(GetWalletCommand command) {
        if (!command.actorId().equals(command.ownerId())) {
            throw new AccessDeniedException(command.actorId(), "view another user's wallet");
        }
        Wallet wallet = wallets.findByOwnerId(command.ownerId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", command.ownerId()));
        if (!wallet.ownerId().equals(command.ownerId())) {
            throw new IllegalStateException("The wallet repository returned a wallet for another owner");
        }
        return new WalletView(wallet.ownerId(), wallet.balance(), wallet.movements());
    }
}
