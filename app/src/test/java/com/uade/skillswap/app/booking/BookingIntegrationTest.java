package com.uade.skillswap.app.booking;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.BookingHistoryEntry;
import com.uade.skillswap.application.dto.BookingView;
import com.uade.skillswap.application.ports.out.BookingPricingPolicy;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.WalletRepository;
import com.uade.skillswap.application.ports.out.publication.PublicationRepository;
import com.uade.skillswap.application.usecases.BookingServiceImpl;
import com.uade.skillswap.controllers.ControllerResult;
import com.uade.skillswap.controllers.ControllerResult.ErrorType;
import com.uade.skillswap.controllers.booking.BookingController;
import com.uade.skillswap.controllers.booking.BookingControllerImpl;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.AccountType;
import com.uade.skillswap.domain.publication.Modality;
import com.uade.skillswap.domain.publication.Publication;
import com.uade.skillswap.domain.publication.PublicationDetails;
import com.uade.skillswap.domain.publication.PublicationStatus;
import com.uade.skillswap.domain.publication.PublicationType;
import com.uade.skillswap.domain.publication.TimeSlot;
import com.uade.skillswap.infrastructure.adapters.PublicationAvailabilityAdapter;
import com.uade.skillswap.infrastructure.adapters.PublicationCatalogAdapter;
import com.uade.skillswap.infrastructure.persistence.EntityManagerContext;
import com.uade.skillswap.infrastructure.persistence.JpaUnitOfWork;
import com.uade.skillswap.infrastructure.persistence.PersistenceConfiguration;
import com.uade.skillswap.infrastructure.persistence.booking.JpaBookingRepository;
import com.uade.skillswap.infrastructure.persistence.publication.JpaPublicationRepository;
import com.uade.skillswap.infrastructure.persistence.wallet.JpaWalletRepository;
import com.uade.skillswap.domain.wallet.FullRefundStrategy;
import com.uade.skillswap.domain.wallet.RefundStrategy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

// Integración de contratación de punta a punta (controller -> caso de uso -> JPA -> H2); solo se simula el login.
class BookingIntegrationTest {

    private static final UserId ANA = new UserId("ana");
    private static final UserId BRUNO = new UserId("bruno");
    private static final UserId CARLA = new UserId("carla");
    private static final Instant NOW = Instant.parse("2026-11-16T10:00:00Z");
    private static final Instant MONDAY = Instant.parse("2026-11-23T15:00:00Z");

    private static final AtomicReference<UserId> ACTIVE = new AtomicReference<>();
    private static final JpaPublicationRepository PUBLICATIONS = new JpaPublicationRepository();
    private static final JpaBookingRepository BOOKINGS = new JpaBookingRepository();
    private static final WalletRepository WALLETS = new JpaWalletRepository();
    private static final BookingPricingPolicy PRICING_POLICY = BookingPricingPolicy.basePrice();
    private static final RefundStrategy REFUND_STRATEGY = new FullRefundStrategy();
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UnitOfWork UNIT_OF_WORK = new JpaUnitOfWork();

    @BeforeAll
    static void startDatabase() {
        PersistenceConfiguration h2 = new PersistenceConfiguration(
                "jdbc:h2:mem:booking-integration;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        h2.migrate();
        EntityManagerContext.init(h2.createEntityManagerFactory());
        UNIT_OF_WORK.execute(() -> EntityManagerContext.withEntityManager(em -> em.createNativeQuery(
                "INSERT INTO category (id, parent_id, name, kind) VALUES (1, NULL, 'Tecnología', 'CATEGORY')")
                .executeUpdate()));
    }

    @AfterAll
    static void closeDatabase() {
        EntityManagerContext.close();
    }

    // Caso de aceptación 3, hasta aceptar: el pago con saldo se suma cuando esté el caso de uso de pagar.
    @Test
    void aClientRequestsAMentoringAndTheProviderAcceptsIt() {
        Publication publication = publishedMentoring(1, 1);
        long slotId = slotIds(publication).get(0);
        BookingController controller = controller(PUBLICATIONS);

        signIn(ANA);
        ControllerResult<BookingView> requested = controller.request("ana", publication.getId(), slotId,
                "Hola Bruno", "Preparar una entrevista técnica");
        assertThat(requested.isSuccess()).isTrue();
        assertThat(requested.getData().status()).isEqualTo(BookingStatus.PENDING);
        assertThat(requested.getData().price()).isEqualTo(100);
        long bookingId = requested.getData().id();

        signIn(BRUNO);
        ControllerResult<BookingView> accepted = controller.accept("bruno", bookingId);
        assertThat(accepted.isSuccess()).isTrue();
        assertThat(accepted.getData().status()).isEqualTo(BookingStatus.ACCEPTED);

        assertThat(PUBLICATIONS.findById(publication.getId()).orElseThrow().getReservedSpots()).isEqualTo(1);
        List<BookingHistoryEntry> history = controller.history("bruno", bookingId).getData();
        assertThat(history).extracting(BookingHistoryEntry::to)
                .containsExactly(BookingStatus.PENDING, BookingStatus.ACCEPTED);
        signIn(ANA);
        Page<BookingView> asClient = controller.listAsClient("ana", 0, PageRequest.DEFAULT_SIZE).getData();
        assertThat(asClient.items()).extracting(BookingView::id).contains(bookingId);
    }

    @Test
    void onlyTheProviderOfTheActiveSessionCanAccept() {
        Publication publication = publishedMentoring(1, 1);
        BookingController controller = controller(PUBLICATIONS);
        signIn(ANA);
        long bookingId = controller.request("ana", publication.getId(), slotIds(publication).get(0), "Hola",
                "Entrevista").getData().id();

        ControllerResult<BookingView> clientAccepts = controller.accept("ana", bookingId);
        ControllerResult<BookingView> notSignedIn = controller.accept("bruno", bookingId);

        assertThat(clientAccepts.isSuccess()).isFalse();
        assertThat(clientAccepts.getMessage()).isEqualTo("Only the provider can accept a booking");
        assertThat(notSignedIn.getErrorType()).isEqualTo(ErrorType.ACCESS_DENIED);
        assertThat(PUBLICATIONS.findById(publication.getId()).orElseThrow().getReservedSpots()).isZero();
    }

    // Caso de aceptación 4: dos aceptaciones simultáneas no consumen el último cupo.
    @Test
    void twoSimultaneousAcceptancesDoNotConsumeTheLastSpot() throws Exception {
        Publication publication = publishedMentoring(1, 2);
        List<Long> slots = slotIds(publication);
        BookingController setup = controller(PUBLICATIONS);
        signIn(ANA);
        long first = setup.request("ana", publication.getId(), slots.get(0), "Hola", "Entrevista").getData().id();
        signIn(CARLA);
        long second = setup.request("carla", publication.getId(), slots.get(1), "Hola", "Entrevista").getData().id();

        CountDownLatch bothLoaded = new CountDownLatch(2);
        BookingController controller = controller(new WaitForBothRepository(PUBLICATIONS, bothLoaded));
        signIn(BRUNO);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<ControllerResult<BookingView>>> outcomes = new ArrayList<>();
        for (long bookingId : List.of(first, second)) {
            outcomes.add(pool.submit(() -> controller.accept("bruno", bookingId)));
        }
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        List<ControllerResult<BookingView>> results = new ArrayList<>();
        for (Future<ControllerResult<BookingView>> outcome : outcomes) {
            results.add(outcome.get());
        }
        assertThat(results).filteredOn(ControllerResult::isSuccess).hasSize(1);
        assertThat(results).filteredOn(result -> !result.isSuccess()).singleElement().satisfies(lost -> {
            assertThat(lost.getErrorType()).isEqualTo(ErrorType.CONFLICT);
            assertThat(lost.getMessage())
                    .isEqualTo("The data was updated by someone else at the same time, please try again");
        });
        assertThat(List.of(status(first), status(second)))
                .containsExactlyInAnyOrder(BookingStatus.ACCEPTED, BookingStatus.PENDING);
        assertThat(PUBLICATIONS.findById(publication.getId()).orElseThrow().getReservedSpots()).isEqualTo(1);
    }

    private static BookingController controller(PublicationRepository publications) {
        return new BookingControllerImpl(new BookingServiceImpl(BOOKINGS, new PublicationCatalogAdapter(publications),
                new PublicationAvailabilityAdapter(publications), WALLETS, PRICING_POLICY, REFUND_STRATEGY,
                UNIT_OF_WORK, CLOCK), ACTIVE::get);
    }

    private static void signIn(UserId user) {
        ACTIVE.set(user);
    }

    private static BookingStatus status(long bookingId) {
        return BOOKINGS.findById(bookingId).orElseThrow().getStatus();
    }

    private static Publication publishedMentoring(int capacity, int slots) {
        Publication publication = Publication.draft(BRUNO, AccountType.PROFESSIONAL, PublicationType.MENTORING,
                new PublicationDetails("Mock interview", "Práctica de entrevista técnica", 1L, Modality.REMOTE,
                        Duration.ofMinutes(60), capacity, new SkillCoins(100), Set.of("java")), NOW);
        for (int i = 0; i < slots; i++) {
            Instant start = MONDAY.plus(Duration.ofHours(2L * i));
            publication.addSlot(BRUNO, start, start.plus(Duration.ofHours(1)));
        }
        publication.publish(BRUNO, NOW);
        Publication saved = UNIT_OF_WORK.execute(() -> PUBLICATIONS.save(publication));
        assertThat(saved.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        return saved;
    }

    private static List<Long> slotIds(Publication publication) {
        return publication.getSlots().stream().map(TimeSlot::getId).sorted().toList();
    }

    // Hace que las dos aceptaciones lean la publicación antes de que cualquiera guarde, así compiten de verdad.
    private record WaitForBothRepository(PublicationRepository delegate, CountDownLatch bothLoaded)
            implements PublicationRepository {

        @Override
        public Optional<Publication> findById(long id) {
            Optional<Publication> publication = delegate.findById(id);
            bothLoaded.countDown();
            try {
                if (!bothLoaded.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("The other acceptance never loaded the publication");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
            return publication;
        }

        @Override
        public Publication save(Publication publication) {
            return delegate.save(publication);
        }

        @Override
        public Page<Publication> findByAuthor(UserId authorId, PublicationStatus status, PageRequest page) {
            return delegate.findByAuthor(authorId, status, page);
        }
    }
}
