package com.uade.skillswap.application.usecases.wallet;

import com.uade.skillswap.application.common.AccessDeniedException;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.dto.wallet.GetWalletCommand;
import com.uade.skillswap.application.dto.wallet.WalletSummaryView;
import com.uade.skillswap.application.dto.wallet.WalletView;
import com.uade.skillswap.application.ports.in.wallet.GetWalletUseCase;
import com.uade.skillswap.application.ports.out.WalletRepository;
import com.uade.skillswap.application.ports.out.WalletStatementQuery;
import com.uade.skillswap.domain.wallet.Wallet;

import java.util.Objects;

public final class GetWalletService implements GetWalletUseCase {

    private final WalletRepository wallets;
    private final WalletStatementQuery statements;

    public GetWalletService(WalletRepository wallets, WalletStatementQuery statements) {
        this.wallets = Objects.requireNonNull(wallets, "The wallet repository is required");
        this.statements = Objects.requireNonNull(statements, "The wallet statement query is required");
    }

    @Override
    public WalletView getWallet(GetWalletCommand command) {
        requireOwner(command);
        Wallet wallet = wallets.findByOwnerId(command.ownerId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", command.ownerId()));
        return WalletView.from(wallet);
    }

    @Override
    public WalletSummaryView getStatement(GetWalletCommand command, PageRequest page) {
        requireOwner(command);
        return statements.statementOf(command.ownerId(), Objects.requireNonNull(page, "The page is required"))
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", command.ownerId()));
    }

    private static void requireOwner(GetWalletCommand command) {
        if (!command.actorId().equals(command.ownerId())) {
            throw new AccessDeniedException(command.actorId(), "view another user's wallet");
        }
    }
}
