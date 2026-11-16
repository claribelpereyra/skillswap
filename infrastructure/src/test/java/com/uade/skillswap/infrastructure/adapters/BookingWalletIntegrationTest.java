package com.uade.skillswap.infrastructure.adapters;

import com.uade.skillswap.application.common.ConcurrentUpdateException;
import com.uade.skillswap.application.dto.BookingView;
import com.uade.skillswap.application.dto.RequestBookingCommand;
import com.uade.skillswap.application.ports.out.BookingPricingPolicy;
import com.uade.skillswap.application.ports.out.WalletRepository;
import com.uade.skillswap.application.usecases.BookingServiceImpl;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.InsufficientBalanceException;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.Publication;
import com.uade.skillswap.domain.wallet.CreditMovementType;
import com.uade.skillswap.domain.wallet.FullRefundStrategy;
import com.uade.skillswap.domain.wallet.RefundPolicy;
import com.uade.skillswap.domain.wallet.RefundStrategy;
import com.uade.skillswap.domain.wallet.TieredRefundStrategy;
import com.uade.skillswap.domain.wallet.Wallet;
import com.uade.skillswap.infrastructure.persistence.PersistenceTestBase;
import com.uade.skillswap.infrastructure.persistence.booking.JpaBookingRepository;
import com.uade.skillswap.infrastructure.persistence.publication.JpaPublicationRepository;
import com.uade.skillswap.infrastructure.persistence.wallet.JpaWalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.uade.skillswap.infrastructure.adapters.PublicationAdaptersTest.NOW;
import static com.uade.skillswap.infrastructure.adapters.PublicationAdaptersTest.PROVIDER;
import static com.uade.skillswap.infrastructure.adapters.PublicationAdaptersTest.publication;
import static com.uade.skillswap.infrastructure.adapters.PublicationAdaptersTest.slotId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingWalletIntegrationTest extends PersistenceTestBase {

    private static final UserId CLIENT = new UserId("ana");
    private static final UserId OTHER_CLIENT = new UserId("carla");
    private static final UserId THIRD_PARTY = new UserId("diana");

    private final JpaPublicationRepository publications = new JpaPublicationRepository();
    private final JpaBookingRepository bookings = new JpaBookingRepository();
    private final JpaWalletRepository wallets = new JpaWalletRepository();
    private Publication persistedPublication;
    private long publicationId;

    @BeforeEach
    void createPublishedPublication() {
        sql("INSERT INTO category (id, parent_id, name, kind) VALUES (?, ?, ?, ?)", 1, null, "Tecnología", "CATEGORY");
        persistedPublication = unitOfWork.execute(() -> publications.save(publication(3, 3, true)));
        publicationId = persistedPublication.getId();
    }

    @Test
    void pagoPersisteLibroYEstadoDeLaContratacionEnUnaTransaccion() {
        BookingServiceImpl service = service(new FullRefundStrategy(), BookingPricingPolicy.basePrice());
        saveWallet(CLIENT, 100);
        saveWallet(PROVIDER, 0);
        long bookingId = requestAndAccept(service, CLIENT, 0);

        BookingView paid = service.pay(bookingId, CLIENT);

        assertThat(paid.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(bookings.findById(bookingId).orElseThrow().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(wallet(CLIENT).balance()).isEqualTo(SkillCoins.ZERO);
        assertThat(wallet(PROVIDER).balance()).isEqualTo(new SkillCoins(100));
        assertThat(wallet(CLIENT).movements()).extracting(movement -> movement.type())
                .containsExactly(CreditMovementType.TOP_UP, CreditMovementType.DEBIT);
        assertThat(wallet(PROVIDER).movements()).extracting(movement -> movement.type())
                .containsExactly(CreditMovementType.CREDIT);
    }

    @Test
    void pagosSimultaneosNoSobregiranLaWalletDesdeSnapshotsObsoletos() throws Exception {
        saveWallet(CLIENT, 100);
        saveWallet(PROVIDER, 0);
        BookingPricingPolicy price80 = (client, publication) -> new SkillCoins(80);
        BookingServiceImpl bookingService = service(new FullRefundStrategy(), price80);
        long firstBooking = requestAndAccept(bookingService, CLIENT, 0);
        long secondBooking = requestAndAccept(bookingService, CLIENT, 1);

        CountDownLatch bothLoaded = new CountDownLatch(2);
        BookingServiceImpl concurrentPayments = service(
                new FullRefundStrategy(),
                price80,
                NOW,
                new WaitForBothClientWallets(wallets, bothLoaded));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<String>> outcomes = List.of(
                    pool.submit(() -> payOutcome(concurrentPayments, firstBooking)),
                    pool.submit(() -> payOutcome(concurrentPayments, secondBooking)));
            pool.shutdown();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

            List<String> results = new ArrayList<>();
            for (Future<String> outcome : outcomes) {
                results.add(outcome.get());
            }
            assertThat(results).containsExactlyInAnyOrder("paid", "conflict");
        } finally {
            pool.shutdownNow();
        }

        assertThat(List.of(bookings.findById(firstBooking).orElseThrow().getStatus(),
                bookings.findById(secondBooking).orElseThrow().getStatus()))
                .containsExactlyInAnyOrder(BookingStatus.CONFIRMED, BookingStatus.ACCEPTED);
        assertThat(wallet(CLIENT).balance()).isEqualTo(new SkillCoins(20));
        assertThat(wallet(PROVIDER).balance()).isEqualTo(new SkillCoins(80));
        assertThat(wallet(CLIENT).movements()).extracting(movement -> movement.type())
                .containsExactly(CreditMovementType.TOP_UP, CreditMovementType.DEBIT);
        assertThat(wallet(PROVIDER).movements()).extracting(movement -> movement.type())
                .containsExactly(CreditMovementType.CREDIT);
    }

    @Test
    void saldoInsuficienteNoModificaWalletsNiContratacion() {
        BookingServiceImpl service = service(new FullRefundStrategy(), BookingPricingPolicy.basePrice());
        saveWallet(CLIENT, 10);
        saveWallet(PROVIDER, 0);
        long bookingId = requestAndAccept(service, CLIENT, 0);

        assertThatThrownBy(() -> service.pay(bookingId, CLIENT))
                .isExactlyInstanceOf(InsufficientBalanceException.class)
                .hasMessage("Insufficient balance: 10 < 100");

        assertThat(bookings.findById(bookingId).orElseThrow().getStatus()).isEqualTo(BookingStatus.ACCEPTED);
        assertThat(wallet(CLIENT).balance()).isEqualTo(new SkillCoins(10));
        assertThat(wallet(PROVIDER).balance()).isEqualTo(SkillCoins.ZERO);
    }

    @Test
    void saldoInsuficienteDelProveedorImpideCancelarOLiberarElCupo() {
        BookingServiceImpl service = service(new FullRefundStrategy(), BookingPricingPolicy.basePrice());
        saveWallet(CLIENT, 100);
        saveWallet(PROVIDER, 0);
        saveWallet(THIRD_PARTY, 0);
        long bookingId = requestAndAccept(service, CLIENT, 0);
        service.pay(bookingId, CLIENT);

        long otherBookingId = service.request(new RequestBookingCommand(
                OTHER_CLIENT, publicationId, slotId(persistedPublication, 1),
                "Hola", "Entrevista")).id();
        unitOfWork.execute(() -> {
            Wallet provider = wallets.findByOwnerId(PROVIDER).orElseThrow();
            Wallet thirdParty = wallets.findByOwnerId(THIRD_PARTY).orElseThrow();
            provider.payBookingTo(thirdParty, new SkillCoins(100), otherBookingId, NOW.plusSeconds(60));
            wallets.save(provider);
            wallets.save(thirdParty);
        });

        BookingServiceImpl cancellationService = service(
                new FullRefundStrategy(), BookingPricingPolicy.basePrice(), NOW.plusSeconds(120));
        assertThatThrownBy(() -> cancellationService.cancel(bookingId, CLIENT, "No puedo asistir"))
                .isExactlyInstanceOf(InsufficientBalanceException.class)
                .hasMessage("Insufficient balance: 0 < 100");

        assertThat(bookings.findById(bookingId).orElseThrow().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(publications.findById(publicationId).orElseThrow().getReservedSpots()).isEqualTo(1);
        assertThat(wallet(CLIENT).balance()).isEqualTo(SkillCoins.ZERO);
        assertThat(wallet(PROVIDER).balance()).isEqualTo(SkillCoins.ZERO);
    }

    @Test
    void cancelarContratacionConfirmadaReintegraYLiberaElCupoJuntos() {
        BookingServiceImpl service = service(new FullRefundStrategy(), BookingPricingPolicy.basePrice());
        saveWallet(CLIENT, 100);
        saveWallet(PROVIDER, 0);
        long bookingId = requestAndAccept(service, CLIENT, 0);
        service.pay(bookingId, CLIENT);

        BookingView cancelled = service.cancel(bookingId, CLIENT, "No puedo asistir");

        assertThat(cancelled.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(bookings.findById(bookingId).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(publications.findById(publicationId).orElseThrow().getReservedSpots()).isZero();
        assertThat(wallet(CLIENT).balance()).isEqualTo(new SkillCoins(100));
        assertThat(wallet(PROVIDER).balance()).isEqualTo(SkillCoins.ZERO);
        assertThat(wallet(CLIENT).movements()).extracting(movement -> movement.type())
                .containsExactly(CreditMovementType.TOP_UP, CreditMovementType.DEBIT, CreditMovementType.REFUND);
        assertThat(wallet(PROVIDER).movements()).extracting(movement -> movement.type())
                .containsExactly(CreditMovementType.CREDIT, CreditMovementType.DEBIT);
    }

    @Test
    void contratacionGratuitaSeConfirmaSinMovimientos() {
        BookingServiceImpl service = service(new FullRefundStrategy(), (client, publication) -> SkillCoins.ZERO);
        long bookingId = requestAndAccept(service, CLIENT, 0);

        BookingView paid = service.pay(bookingId, CLIENT);

        assertThat(paid.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(paid.price()).isZero();
        assertThat(wallets.findByOwnerId(CLIENT)).isEmpty();
        assertThat(wallets.findByOwnerId(PROVIDER)).isEmpty();
    }

    private BookingServiceImpl service(RefundStrategy refunds, BookingPricingPolicy pricing) {
        return service(refunds, pricing, NOW);
    }

    private BookingServiceImpl service(RefundStrategy refunds, BookingPricingPolicy pricing, java.time.Instant now) {
        return service(refunds, pricing, now, wallets);
    }

    private BookingServiceImpl service(
            RefundStrategy refunds, BookingPricingPolicy pricing, java.time.Instant now, WalletRepository walletRepo) {
        return new BookingServiceImpl(
                bookings,
                new PublicationCatalogAdapter(publications),
                new PublicationAvailabilityAdapter(publications),
                walletRepo,
                pricing,
                refunds,
                unitOfWork,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    private String payOutcome(BookingServiceImpl service, long bookingId) {
        try {
            service.pay(bookingId, CLIENT);
            return "paid";
        } catch (ConcurrentUpdateException conflict) {
            return "conflict";
        }
    }

    private long requestAndAccept(BookingServiceImpl service, UserId client, int slotIndex) {
        BookingView requested = service.request(new RequestBookingCommand(
                client, publicationId, slotId(persistedPublication, slotIndex),
                "Hola", "Entrevista"));
        service.accept(requested.id(), PROVIDER);
        return requested.id();
    }

    private void saveWallet(UserId owner, long balance) {
        Wallet wallet = Wallet.open(owner);
        if (balance > 0) {
            wallet.topUp(new SkillCoins(balance), NOW.minusSeconds(60));
        }
        unitOfWork.execute(() -> wallets.save(wallet));
    }

    private Wallet wallet(UserId owner) {
        return wallets.findByOwnerId(owner).orElseThrow();
    }

    private static final class WaitForBothClientWallets implements WalletRepository {

        private final WalletRepository delegate;
        private final CountDownLatch bothLoaded;

        private WaitForBothClientWallets(WalletRepository delegate, CountDownLatch bothLoaded) {
            this.delegate = delegate;
            this.bothLoaded = bothLoaded;
        }

        @Override
        public Wallet save(Wallet wallet) {
            return delegate.save(wallet);
        }

        @Override
        public Optional<Wallet> findByOwnerId(UserId ownerId) {
            Optional<Wallet> result = delegate.findByOwnerId(ownerId);
            if (CLIENT.equals(ownerId)) {
                bothLoaded.countDown();
                try {
                    if (!bothLoaded.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("The other payment did not load the client wallet");
                    }
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(error);
                }
            }
            return result;
        }
    }
}
