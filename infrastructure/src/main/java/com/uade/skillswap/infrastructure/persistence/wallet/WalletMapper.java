package com.uade.skillswap.infrastructure.persistence.wallet;

import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.wallet.CreditMovement;
import com.uade.skillswap.domain.wallet.Wallet;

import java.util.List;
import java.util.Objects;

final class WalletMapper {

    private WalletMapper() {
    }

    static Wallet toDomain(WalletJpaEntity entity) {
        List<CreditMovement> movements = entity.getMovements().stream()
                .map(movement -> new CreditMovement(movement.getMovementType(),
                        new SkillCoins(movement.getAmount()), movement.getOccurredAt(), movement.getBookingId()))
                .toList();
        return Wallet.restore(new UserId(entity.getOwnerId()), new SkillCoins(entity.getBalance()), movements);
    }

    static WalletJpaEntity newEntity(Wallet wallet) {
        WalletJpaEntity entity = new WalletJpaEntity(wallet.ownerId().value(), 0);
        copyInto(wallet, entity);
        return entity;
    }

    static void copyInto(Wallet wallet, WalletJpaEntity entity) {
        copyBalance(wallet, entity);
        appendMovements(wallet, entity);
    }

    static void copyBalance(Wallet wallet, WalletJpaEntity entity) {
        if (!wallet.ownerId().value().equals(entity.getOwnerId())) {
            throw new IllegalArgumentException("The wallet owner cannot be changed");
        }
        entity.setBalance(wallet.balance().amount());
    }

    static void appendMovements(Wallet wallet, WalletJpaEntity entity) {
        if (!wallet.ownerId().value().equals(entity.getOwnerId())) {
            throw new IllegalArgumentException("The wallet owner cannot be changed");
        }
        List<CreditMovement> movements = wallet.movements();
        List<WalletMovementJpaEntity> stored = entity.getMovements();
        if (stored.size() > movements.size()) {
            throw new IllegalStateException("Wallet movements cannot be removed");
        }
        for (int i = 0; i < stored.size(); i++) {
            WalletMovementJpaEntity persisted = stored.get(i);
            CreditMovement current = movements.get(i);
            if (persisted.getSequenceNumber() != i
                    || persisted.getMovementType() != current.type()
                    || persisted.getAmount() != current.amount().amount()
                    || !persisted.getOccurredAt().equals(current.occurredAt())
                    || !Objects.equals(persisted.getBookingId(), current.bookingId())) {
                throw new IllegalStateException("Wallet movements are immutable");
            }
        }
        for (int i = stored.size(); i < movements.size(); i++) {
            CreditMovement movement = movements.get(i);
            stored.add(new WalletMovementJpaEntity(entity, i, movement.type(), movement.amount().amount(),
                    movement.occurredAt(), movement.bookingId()));
        }
    }
}
