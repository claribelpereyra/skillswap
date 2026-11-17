package com.uade.skillswap.controllers.wallet;

import com.uade.skillswap.application.common.AccessDeniedException;
import com.uade.skillswap.application.dto.wallet.GetWalletCommand;
import com.uade.skillswap.application.dto.wallet.WalletSummaryView;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.ports.in.wallet.GetWalletUseCase;
import com.uade.skillswap.application.ports.out.IdentityService;
import com.uade.skillswap.controllers.ControllerResult;
import java.util.Objects;

public final class WalletQueryControllerImpl implements WalletQueryController {
    private final GetWalletUseCase query;
    private final IdentityService identity;

    public WalletQueryControllerImpl(GetWalletUseCase query, IdentityService identity) {
        this.query = Objects.requireNonNull(query);
        this.identity = Objects.requireNonNull(identity);
    }

    @Override
    public ControllerResult<WalletSummaryView> getMyWallet(int page, int size) {
        return ControllerResult.of(() -> {
            var actor = identity.currentUserId();
            if (actor == null) throw new AccessDeniedException("No active user");
            return query.getStatement(new GetWalletCommand(actor, actor), new PageRequest(page, size));
        });
    }
}
