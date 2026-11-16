package com.uade.skillswap.infrastructure.persistence.wallet;

import com.uade.skillswap.application.common.ConcurrentUpdateException;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.wallet.Wallet;
import com.uade.skillswap.infrastructure.persistence.PersistenceTestBase;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class JpaWalletRepositoryTest extends PersistenceTestBase {

    private static final UserId OWNER = new UserId("ana");
    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");

    private final JpaWalletRepository repository = new JpaWalletRepository();

    @Test
    void guardaYRestauraSaldoYMovimientos() {
        Wallet wallet = Wallet.open(OWNER);
        wallet.topUp(new SkillCoins(100), NOW);

        unitOfWork.execute(() -> repository.save(wallet));

        Wallet restored = repository.findByOwnerId(OWNER).orElseThrow();
        assertThat(restored.balance()).isEqualTo(new SkillCoins(100));
        assertThat(restored.movements()).hasSize(1);
        assertThat(restored.movements().get(0).amount()).isEqualTo(new SkillCoins(100));
    }

    @Test
    void conflictoOptimistaRechazaCambiosDesdeSnapshotsObsoletos() throws Exception {
        Wallet wallet = Wallet.open(OWNER);
        wallet.topUp(new SkillCoins(100), NOW);
        unitOfWork.execute(() -> repository.save(wallet));

        CountDownLatch bothLoaded = new CountDownLatch(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<String>> outcomes = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                outcomes.add(pool.submit(() -> {
                    try {
                        unitOfWork.execute(() -> {
                            Wallet snapshot = repository.findByOwnerId(OWNER).orElseThrow();
                            bothLoaded.countDown();
                            awaitBothLoaded(bothLoaded);
                            snapshot.topUp(new SkillCoins(1), NOW.plusSeconds(60));
                            repository.save(snapshot);
                            return null;
                        });
                        return "saved";
                    } catch (ConcurrentUpdateException conflict) {
                        return "conflict";
                    }
                }));
            }

            pool.shutdown();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
            List<String> results = new ArrayList<>();
            for (Future<String> outcome : outcomes) {
                results.add(outcome.get());
            }
            assertThat(results).containsExactlyInAnyOrder("saved", "conflict");
        } finally {
            pool.shutdownNow();
        }

        Wallet restored = repository.findByOwnerId(OWNER).orElseThrow();
        assertThat(restored.balance()).isEqualTo(new SkillCoins(101));
        assertThat(restored.movements()).hasSize(2);
    }

    private static void awaitBothLoaded(CountDownLatch bothLoaded) {
        try {
            if (!bothLoaded.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("The other wallet update did not load its snapshot");
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(error);
        }
    }
}
