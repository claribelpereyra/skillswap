package com.uade.skillswap.infrastructure.persistence.wallet;

import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.wallet.WalletMovementView;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.wallet.Wallet;
import com.uade.skillswap.infrastructure.persistence.PersistenceTestBase;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JpaWalletStatementQueryTest extends PersistenceTestBase {

    private static final UserId OWNER = new UserId("ana");
    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");

    private final JpaWalletRepository wallets = new JpaWalletRepository();
    private final JpaWalletStatementQuery statements = new JpaWalletStatementQuery();

    @Test
    void returnsTheStoredBalanceAndOnlyTheRequestedPageOfMovementsInOrder() {
        Wallet wallet = Wallet.open(OWNER);
        for (int i = 1; i <= 5; i++) {
            wallet.topUp(new SkillCoins(i * 10L), NOW.plusSeconds(i));
        }
        unitOfWork.execute(() -> wallets.save(wallet));

        var statement = unitOfWork.execute(() -> statements.statementOf(OWNER, new PageRequest(1, 2))).orElseThrow();

        assertThat(statement.ownerId()).isEqualTo("ana");
        assertThat(statement.balance()).isEqualTo(150);
        assertThat(statement.movements().totalItems()).isEqualTo(5);
        assertThat(statement.movements().items()).extracting(WalletMovementView::amount).containsExactly(30L, 40L);
        assertThat(unitOfWork.execute(() -> statements.statementOf(new UserId("nobody"), PageRequest.first())))
                .isEmpty();
    }
}
