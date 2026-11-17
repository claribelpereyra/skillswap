package com.uade.skillswap.infrastructure.persistence.wallet;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.wallet.WalletMovementView;
import com.uade.skillswap.application.dto.wallet.WalletSummaryView;
import com.uade.skillswap.application.ports.out.WalletStatementQuery;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.infrastructure.persistence.EntityManagerContext;

import java.util.Optional;

// Tres consultas fijas: el saldo guardado en la fila, el total de movimientos y solo la página pedida.
public final class JpaWalletStatementQuery implements WalletStatementQuery {

    @Override
    public Optional<WalletSummaryView> statementOf(UserId ownerId, PageRequest page) {
        return EntityManagerContext.withEntityManager(em -> em.createQuery(
                        "select w from WalletJpaEntity w where w.ownerId = :ownerId", WalletJpaEntity.class)
                .setParameter("ownerId", ownerId.value())
                .getResultStream()
                .findFirst()
                .map(wallet -> {
                    long total = em.createQuery(
                                    "select count(m) from WalletMovementJpaEntity m where m.wallet = :wallet", Long.class)
                            .setParameter("wallet", wallet)
                            .getSingleResult();
                    var items = em.createQuery("select m from WalletMovementJpaEntity m where m.wallet = :wallet"
                                    + " order by m.occurredAt, m.id", WalletMovementJpaEntity.class)
                            .setParameter("wallet", wallet)
                            .setFirstResult(Math.toIntExact(page.offset()))
                            .setMaxResults(page.size())
                            .getResultStream()
                            .map(movement -> new WalletMovementView(movement.getType().name(), movement.getAmount(),
                                    movement.getOccurredAt(), movement.getBookingId()))
                            .toList();
                    return new WalletSummaryView(wallet.getOwnerId(), wallet.getBalance(),
                            new Page<>(items, page, total));
                }));
    }
}
