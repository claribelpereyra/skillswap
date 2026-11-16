package com.uade.skillswap.controllers.wallet;

import com.uade.skillswap.application.dto.wallet.WalletView;
import com.uade.skillswap.controllers.ControllerResult;

public interface WalletController {

    ControllerResult<WalletView> loadCredits(String userId, Long amount);
}
