package com.uade.skillswap.application.ports.in.wallet;

public interface GetWalletUseCase {

    WalletView getWallet(GetWalletCommand command);
}
