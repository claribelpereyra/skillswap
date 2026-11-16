package com.uade.skillswap.infrastructure.persistence.wallet;

import com.uade.skillswap.application.ports.out.WalletRepository;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.wallet.Wallet;
import com.uade.skillswap.infrastructure.persistence.EntityManagerContext;
import jakarta.persistence.EntityManager;

import java.util.Optional;

public class JpaWalletRepository implements WalletRepository {

    @Override
    public Wallet save(Wallet wallet) {
        return EntityManagerContext.withEntityManager(em -> {
            WalletJpaEntity entity = findEntity(em, wallet.ownerId())
                    .orElseGet(() -> WalletJpaEntity.create(wallet.ownerId().value()));
            WalletMapper.copyToEntity(wallet, entity);
            if (entity.getId() == null) {
                em.persist(entity);
            }
            em.flush();
            return WalletMapper.toDomain(entity);
        });
    }

    @Override
    public Optional<Wallet> findByOwnerId(UserId ownerId) {
        return EntityManagerContext.withEntityManager(em ->
                findEntity(em, ownerId).map(WalletMapper::toDomain));
    }

    private Optional<WalletJpaEntity> findEntity(EntityManager em, UserId ownerId) {
        return em.createQuery(
                        "select distinct w from WalletJpaEntity w left join fetch w.movements where w.ownerId = :ownerId",
                        WalletJpaEntity.class)
                .setParameter("ownerId", ownerId.value())
                .getResultStream()
                .findFirst();
    }
}
