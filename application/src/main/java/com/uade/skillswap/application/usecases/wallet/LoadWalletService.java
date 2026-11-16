package com.uade.skillswap.application.usecases.wallet;

import com.uade.skillswap.application.dto.wallet.WalletView;
import com.uade.skillswap.application.ports.in.wallet.LoadWalletCommand;
import com.uade.skillswap.application.ports.in.wallet.LoadWalletUseCase;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.SkillCoinGateway;
import com.uade.skillswap.application.ports.out.WalletRepository;
import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.wallet.Wallet;

import java.time.Clock;
import java.util.Objects;

public final class LoadWalletService implements LoadWalletUseCase {

    private final WalletRepository wallets;
    private final SkillCoinGateway gateway;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public LoadWalletService(
            WalletRepository wallets,
            SkillCoinGateway gateway,
            UnitOfWork unitOfWork,
            Clock clock) {
        this.wallets = Objects.requireNonNull(wallets, "The wallet repository is required");
        this.gateway = Objects.requireNonNull(gateway, "The SkillCoins gateway is required");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "The unit of work is required");
        this.clock = Objects.requireNonNull(clock, "The clock is required");
    }

    @Override
    public WalletView loadCredits(LoadWalletCommand command) {
        Objects.requireNonNull(command, "The wallet load command is required");
        if (!gateway.authorizeCredit(command.userId(), command.amount())) {
            throw new BusinessRuleException("The simulated payment was declined");
        }
        return unitOfWork.execute(() -> {
            Wallet wallet = wallets.findByOwnerId(command.userId())
                    .orElseGet(() -> Wallet.open(command.userId()));
            wallet.topUp(command.amount(), clock.instant());
            wallets.save(wallet);
            return WalletView.from(wallet);
        });
    }
}
