package com.uade.skillswap.application.ports.in.wallet;

import com.uade.skillswap.application.dto.wallet.WalletView;

public interface LoadWalletUseCase {

    WalletView loadCredits(LoadWalletCommand command);
}
