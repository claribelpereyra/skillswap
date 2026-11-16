package com.uade.skillswap.controllers.wallet;

import com.uade.skillswap.application.common.AccessDeniedException;
import com.uade.skillswap.application.dto.wallet.WalletView;
import com.uade.skillswap.application.ports.in.wallet.LoadWalletCommand;
import com.uade.skillswap.application.ports.in.wallet.LoadWalletUseCase;
import com.uade.skillswap.application.ports.out.IdentityService;
import com.uade.skillswap.controllers.ControllerResult;
import com.uade.skillswap.controllers.common.ControllerSupport.InvalidInputException;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;

import java.util.Objects;

public final class WalletControllerImpl implements WalletController {

    private final LoadWalletUseCase loadWallet;
    private final IdentityService identity;

    public WalletControllerImpl(LoadWalletUseCase loadWallet, IdentityService identity) {
        this.loadWallet = Objects.requireNonNull(loadWallet, "The wallet load use case is required");
        this.identity = Objects.requireNonNull(identity, "The identity service is required");
    }

    @Override
    public ControllerResult<WalletView> loadCredits(String userId, Long amount) {
        return ControllerResult.of(() -> {
            UserId user = currentUser(userId);
            if (amount == null) {
                throw new InvalidInputException("The credit amount is required");
            }
            return loadWallet.loadCredits(new LoadWalletCommand(user, new SkillCoins(amount)));
        });
    }

    private UserId currentUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new AccessDeniedException("No active user");
        }
        UserId user = new UserId(userId);
        if (!user.equals(identity.currentUserId())) {
            throw new AccessDeniedException(user, "load wallet credits");
        }
        return user;
    }
}
