package com.uade.skillswap.application.dto.wallet;

import com.uade.skillswap.application.common.Page;

/** Saldo y movimientos paginados para la pantalla de wallet. */
public record WalletSummaryView(String ownerId, long balance, Page<WalletMovementView> movements) { }
