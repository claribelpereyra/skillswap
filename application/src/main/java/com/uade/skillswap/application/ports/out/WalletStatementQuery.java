package com.uade.skillswap.application.ports.out;

import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.wallet.WalletSummaryView;
import com.uade.skillswap.domain.common.UserId;

import java.util.Optional;

// Lectura de la wallet: saldo y una página de movimientos resueltos en la base, sin cargar el historial completo.
public interface WalletStatementQuery {

    Optional<WalletSummaryView> statementOf(UserId ownerId, PageRequest page);
}
