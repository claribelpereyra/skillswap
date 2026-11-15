package com.uade.skillswap.infrastructure.persistence.wallet;

import com.uade.skillswap.application.ports.out.wallet.WalletRepository;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.wallet.Wallet;
import com.uade.skillswap.infrastructure.persistence.EntityManagerContext;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public class JpaWalletRepository implements WalletRepository {

    @Override
    public Optional<Wallet> findByOwnerId(UserId ownerId) {
        return EntityManagerContext.withEntityManager(em -> em.createQuery(
                        "SELECT DISTINCT w FROM WalletJpaEntity w LEFT JOIN FETCH w.movements WHERE w.ownerId = :owner",
                        WalletJpaEntity.class)
                .setParameter("owner", ownerId.value())
                .getResultStream()
                .findFirst()
                .map(WalletMapper::toDomain));
    }

    @Override
    public void save(Wallet wallet) {
        EntityManagerContext.withEntityManager(em -> {
            WalletJpaEntity entity = em.find(WalletJpaEntity.class, wallet.ownerId().value());
            if (entity == null) {
                em.persist(WalletMapper.newEntity(wallet));
            } else {
                WalletMapper.copyBalance(wallet, entity);
                em.lock(entity, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
                em.flush();
                WalletMapper.appendMovements(wallet, entity);
            }
            em.flush();
            return null;
        });
    }
}
