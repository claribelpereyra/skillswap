package com.uade.skillswap.infrastructure.persistence.wallet;

import com.uade.skillswap.application.common.ConcurrentUpdateException;
import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.profile.AccountType;
import com.uade.skillswap.domain.publication.Modality;
import com.uade.skillswap.domain.publication.Publication;
import com.uade.skillswap.domain.publication.PublicationDetails;
import com.uade.skillswap.domain.publication.PublicationType;
import com.uade.skillswap.domain.publication.TimeSlot;
import com.uade.skillswap.domain.wallet.BookingPaymentService;
import com.uade.skillswap.domain.wallet.CreditMovementType;
import com.uade.skillswap.domain.wallet.RefundPolicy;
import com.uade.skillswap.domain.wallet.TieredRefundStrategy;
import com.uade.skillswap.domain.wallet.Wallet;
import com.uade.skillswap.infrastructure.persistence.PersistenceTestBase;
import com.uade.skillswap.infrastructure.persistence.booking.JpaBookingRepository;
import com.uade.skillswap.infrastructure.persistence.publication.JpaPublicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JpaWalletRepositoryTest extends PersistenceTestBase {

    private static final UserId CLIENT = new UserId("ana");
    private static final UserId PROVIDER = new UserId("bruno");
    private static final Instant NOW = Instant.parse("2026-11-15T10:00:00Z");
    private static final SkillCoins PRICE = new SkillCoins(35);

    private final JpaWalletRepository wallets = new JpaWalletRepository();
    private final JpaBookingRepository bookings = new JpaBookingRepository();
    private final JpaPublicationRepository publications = new JpaPublicationRepository();
    private final BookingPaymentService payments = new BookingPaymentService(
            new TieredRefundStrategy(new RefundPolicy(Duration.ofDays(2), Duration.ofDays(1))));
    private long bookingId;
    private long publicationId;
    private TimeSlot bookingSlot;

    @BeforeEach
    void seedBooking() {
        sql("INSERT INTO category (id, parent_id, name, kind) VALUES (?, ?, ?, ?)",
                1, null, "Tecnología", "CATEGORY");
        Publication publication = Publication.draft(PROVIDER, AccountType.PROFESSIONAL, PublicationType.MENTORING,
                new PublicationDetails("Mock interview", "Practice an interview", 1L, Modality.REMOTE,
                        Duration.ofMinutes(60), 2, new SkillCoins(100), Set.of("java")), NOW);
        publication.addSlot(PROVIDER, NOW.plus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(1)).plus(Duration.ofHours(1)));
        Publication savedPublication = unitOfWork.execute(() -> publications.save(publication));
        publicationId = savedPublication.getId();
        bookingSlot = savedPublication.getSlots().get(0);
        bookingId = unitOfWork.execute(() -> bookings.save(
                Booking.request(publicationId, CLIENT, PROVIDER, bookingSlot,
                        "Wallet persistence test", "Payment", PRICE)).getId());
    }

    @Test
    void walletAndMovementLedgerRoundTripAndSupportRefundAfterReload() {
        seedWallets();
        payStoredBooking();

        Wallet persistedClient = load(CLIENT);
        Wallet persistedProvider = load(PROVIDER);
        assertThat(persistedClient.balance()).isEqualTo(new SkillCoins(65));
        assertThat(persistedProvider.balance()).isEqualTo(PRICE);
        assertThat(persistedClient.movements()).extracting(movement -> movement.type())
                .containsExactly(CreditMovementType.TOP_UP, CreditMovementType.DEBIT);
        assertThat(persistedProvider.movements()).extracting(movement -> movement.type())
                .containsExactly(CreditMovementType.CREDIT);

        unitOfWork.execute(() -> {
            Wallet client = wallets.findByOwnerId(CLIENT).orElseThrow();
            Wallet provider = wallets.findByOwnerId(PROVIDER).orElseThrow();
            SkillCoins refund = payments.refund(bookingId, client, provider, NOW.plus(Duration.ofDays(3)),
                    NOW.plusSeconds(120));
            assertThat(refund).isEqualTo(PRICE);
            wallets.save(client);
            wallets.save(provider);
            return null;
        });

        Wallet refundedClient = load(CLIENT);
        Wallet refundedProvider = load(PROVIDER);
        assertThat(refundedClient.balance()).isEqualTo(new SkillCoins(100));
        assertThat(refundedProvider.balance()).isEqualTo(SkillCoins.ZERO);
        assertThat(refundedClient.movements()).last().extracting(movement -> movement.type())
                .isEqualTo(CreditMovementType.REFUND);
        assertThat(refundedProvider.movements()).last().extracting(movement -> movement.type())
                .isEqualTo(CreditMovementType.DEBIT);
        assertThatThrownBy(() -> payments.refund(bookingId, refundedClient, refundedProvider,
                NOW.plus(Duration.ofDays(3)), NOW.plusSeconds(180)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void aFailedPaymentTransactionRollsBackBothWalletsAndTheirMovements() {
        seedWallets();

        assertThatThrownBy(() -> unitOfWork.execute(() -> {
            Wallet client = wallets.findByOwnerId(CLIENT).orElseThrow();
            Wallet provider = wallets.findByOwnerId(PROVIDER).orElseThrow();
            payments.pay(bookingId, client, provider, PRICE, NOW.plusSeconds(60));
            wallets.save(client);
            wallets.save(provider);
            throw new IllegalStateException("force transaction rollback");
        })).isInstanceOf(IllegalStateException.class).hasMessage("force transaction rollback");

        assertThat(load(CLIENT).balance()).isEqualTo(new SkillCoins(100));
        assertThat(load(CLIENT).movements()).hasSize(1);
        assertThat(load(PROVIDER).balance()).isEqualTo(SkillCoins.ZERO);
        assertThat(load(PROVIDER).movements()).isEmpty();
    }

    @Test
    void aFailedCancellationTransactionRollsBackBothRefundSides() {
        seedWallets();
        payStoredBooking();

        assertThatThrownBy(() -> unitOfWork.execute(() -> {
            Wallet client = wallets.findByOwnerId(CLIENT).orElseThrow();
            Wallet provider = wallets.findByOwnerId(PROVIDER).orElseThrow();
            payments.refund(bookingId, client, provider, NOW.plus(Duration.ofDays(3)), NOW.plusSeconds(120));
            wallets.save(client);
            wallets.save(provider);
            throw new IllegalStateException("force transaction rollback");
        })).isInstanceOf(IllegalStateException.class).hasMessage("force transaction rollback");

        assertThat(load(CLIENT).balance()).isEqualTo(new SkillCoins(65));
        assertThat(load(CLIENT).movements()).hasSize(2);
        assertThat(load(PROVIDER).balance()).isEqualTo(PRICE);
        assertThat(load(PROVIDER).movements()).hasSize(1);
    }

    @Test
    void concurrentPaymentsFromTheSameLoadedWalletProduceAConflict() throws Exception {
        UserId otherProvider = new UserId("carla");
        long firstBookingId = bookingId;
        long secondBookingId = createBooking();
        Wallet client = Wallet.open(CLIENT);
        client.topUp(new SkillCoins(100), NOW);
        unitOfWork.execute(() -> {
            wallets.save(client);
            wallets.save(Wallet.open(PROVIDER));
            wallets.save(Wallet.open(otherProvider));
            return null;
        });

        CountDownLatch bothLoaded = new CountDownLatch(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<String> first = pool.submit(() -> payConcurrently(PROVIDER, firstBookingId, bothLoaded));
        Future<String> second = pool.submit(() -> payConcurrently(otherProvider, secondBookingId, bothLoaded));
        pool.shutdown();

        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder("saved", "conflict");
        assertThat(load(CLIENT).balance()).isEqualTo(new SkillCoins(20));
        assertThat(load(CLIENT).movements()).extracting(movement -> movement.type())
                .containsExactly(CreditMovementType.TOP_UP, CreditMovementType.DEBIT);
        assertThat(load(PROVIDER).balance().amount() + load(otherProvider).balance().amount()).isEqualTo(80);
    }

    private String payConcurrently(UserId providerOwner, long bookingId, CountDownLatch bothLoaded) {
        try {
            unitOfWork.execute(() -> {
                Wallet client = wallets.findByOwnerId(CLIENT).orElseThrow();
                Wallet provider = wallets.findByOwnerId(providerOwner).orElseThrow();
                bothLoaded.countDown();
                awaitBothPayments(bothLoaded);
                client.payBookingTo(provider, new SkillCoins(80), bookingId, NOW.plusSeconds(60));
                wallets.save(client);
                wallets.save(provider);
                return null;
            });
            return "saved";
        } catch (ConcurrentUpdateException conflict) {
            return "conflict";
        }
    }

    private void awaitBothPayments(CountDownLatch bothLoaded) {
        try {
            if (!bothLoaded.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("The other payment did not load the wallet");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }

    private void seedWallets() {
        Wallet client = Wallet.open(CLIENT);
        client.topUp(new SkillCoins(100), NOW);
        Wallet provider = Wallet.open(PROVIDER);
        unitOfWork.execute(() -> {
            wallets.save(client);
            wallets.save(provider);
            return null;
        });
    }

    private void payStoredBooking() {
        unitOfWork.execute(() -> {
            Wallet client = wallets.findByOwnerId(CLIENT).orElseThrow();
            Wallet provider = wallets.findByOwnerId(PROVIDER).orElseThrow();
            payments.pay(bookingId, client, provider, PRICE, NOW.plusSeconds(60));
            wallets.save(client);
            wallets.save(provider);
            return null;
        });
    }

    private long createBooking() {
        return unitOfWork.execute(() -> bookings.save(Booking.request(
                publicationId, CLIENT, PROVIDER, bookingSlot,
                "Concurrent wallet test", "Payment", PRICE)).getId());
    }

    private Wallet load(UserId owner) {
        return unitOfWork.execute(() -> wallets.findByOwnerId(owner).orElseThrow());
    }
}
