package com.uade.skillswap.application.ports.in.wallet;

import com.uade.skillswap.application.dto.wallet.GetWalletCommand;
import com.uade.skillswap.application.dto.wallet.WalletView;

public interface GetWalletUseCase {

    WalletView getWallet(GetWalletCommand command);
}
