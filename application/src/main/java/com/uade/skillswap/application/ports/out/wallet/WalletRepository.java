package com.uade.skillswap.application.ports.out.wallet;

import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.wallet.Wallet;

import java.util.Optional;

public interface WalletRepository {

    Optional<Wallet> findByOwnerId(UserId ownerId);

    void save(Wallet wallet);
}
