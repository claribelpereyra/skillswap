package com.uade.skillswap.infrastructure.persistence.wallet;

import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.wallet.CreditMovement;
import com.uade.skillswap.domain.wallet.Wallet;

import java.util.List;

final class WalletMapper {

    private WalletMapper() {
    }

    static Wallet toDomain(WalletJpaEntity entity) {
        List<CreditMovement> movements = entity.getMovements().stream()
                .map(movement -> new CreditMovement(
                        movement.getType(),
                        new SkillCoins(movement.getAmount()),
                        movement.getOccurredAt(),
                        movement.getBookingId()))
                .toList();
        Wallet wallet = Wallet.restore(new UserId(entity.getOwnerId()), movements);
        if (wallet.balance().amount() != entity.getBalance()) {
            throw new IllegalStateException("The persisted wallet balance does not match its ledger");
        }
        return wallet;
    }

    static void copyToEntity(Wallet wallet, WalletJpaEntity entity) {
        if (!wallet.ownerId().value().equals(entity.getOwnerId())) {
            throw new IllegalArgumentException("A wallet cannot be saved under a different owner");
        }

        List<CreditMovement> movements = wallet.movements();
        List<WalletMovementJpaEntity> stored = entity.getMovements();
        if (movements.size() < stored.size()) {
            throw new IllegalArgumentException("Wallet movements cannot be removed");
        }
        for (int i = 0; i < stored.size(); i++) {
            if (!toDomain(stored.get(i)).equals(movements.get(i))) {
                throw new IllegalArgumentException("Wallet movements cannot be changed");
            }
        }
        for (int i = stored.size(); i < movements.size(); i++) {
            CreditMovement movement = movements.get(i);
            entity.addMovement(new WalletMovementJpaEntity(
                    movement.type(), movement.amount().amount(), movement.occurredAt(), movement.bookingId()));
        }
        entity.setBalance(wallet.balance().amount());
    }

    private static CreditMovement toDomain(WalletMovementJpaEntity movement) {
        return new CreditMovement(
                movement.getType(),
                new SkillCoins(movement.getAmount()),
                movement.getOccurredAt(),
                movement.getBookingId());
    }
}
