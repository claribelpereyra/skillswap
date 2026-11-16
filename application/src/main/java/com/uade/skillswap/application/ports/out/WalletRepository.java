package com.uade.skillswap.application.ports.out;

import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.wallet.Wallet;

import java.util.Optional;

public interface WalletRepository {

    Wallet save(Wallet wallet);

    Optional<Wallet> findByOwnerId(UserId ownerId);
}
