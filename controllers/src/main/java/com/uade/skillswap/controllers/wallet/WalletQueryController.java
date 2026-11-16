package com.uade.skillswap.controllers.wallet;

import com.uade.skillswap.application.dto.wallet.WalletSummaryView;
import com.uade.skillswap.controllers.ControllerResult;

public interface WalletQueryController {
    ControllerResult<WalletSummaryView> getMyWallet(int page, int size);
    default ControllerResult<WalletSummaryView> getMyWallet() { return getMyWallet(0, 20); }
}
