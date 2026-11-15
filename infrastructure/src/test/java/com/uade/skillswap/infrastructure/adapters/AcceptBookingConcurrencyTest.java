package com.uade.skillswap.infrastructure.adapters;

import com.uade.skillswap.application.common.ConcurrentUpdateException;
import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.ports.out.publication.PublicationRepository;
import com.uade.skillswap.application.ports.out.wallet.WalletRepository;
import com.uade.skillswap.application.usecases.BookingServiceImpl;
import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.Publication;
import com.uade.skillswap.domain.publication.PublicationStatus;
import com.uade.skillswap.domain.publication.TimeSlot;
import com.uade.skillswap.domain.wallet.BookingPaymentService;
import com.uade.skillswap.domain.wallet.FullRefundStrategy;
import com.uade.skillswap.infrastructure.persistence.PersistenceTestBase;
import com.uade.skillswap.infrastructure.persistence.booking.JpaBookingRepository;
import com.uade.skillswap.infrastructure.persistence.publication.JpaPublicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.time.Clock;

import static com.uade.skillswap.infrastructure.adapters.PublicationAdaptersTest.PROVIDER;
import static com.uade.skillswap.infrastructure.adapters.PublicationAdaptersTest.publication;
import static com.uade.skillswap.infrastructure.adapters.PublicationAdaptersTest.slotId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

// Caso de aceptación 4: dos aceptaciones simultáneas no consumen el último cupo.
class AcceptBookingConcurrencyTest extends PersistenceTestBase {

    private final JpaPublicationRepository publications = new JpaPublicationRepository();
    private final JpaBookingRepository bookings = new JpaBookingRepository();

    @BeforeEach
    void category() {
        sql("INSERT INTO category (id, parent_id, name, kind) VALUES (?, ?, ?, ?)", 1, null, "Tecnología", "CATEGORY");
    }

    @Test
    void twoSimultaneousAcceptancesCannotBothTakeTheLastSpot() throws Exception {
        Publication publication = unitOfWork.execute(() -> publications.save(publication(1, 2, true)));
        long first = pendingBooking(publication, new UserId("ana"), slotId(publication, 0));
        long second = pendingBooking(publication, new UserId("carla"), slotId(publication, 1));

        CountDownLatch bothLoaded = new CountDownLatch(2);
        BookingServiceImpl service = new BookingServiceImpl(bookings,
                new PublicationCatalogAdapter(publications),
                new PublicationAvailabilityAdapter(new WaitForBothRepository(publications, bothLoaded)), unitOfWork,
                mock(WalletRepository.class), new BookingPaymentService(new FullRefundStrategy()), Clock.systemUTC());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<String>> outcomes = new ArrayList<>();
        for (long bookingId : List.of(first, second)) {
            outcomes.add(pool.submit(() -> {
                try {
                    service.accept(bookingId, PROVIDER);
                    return "accepted";
                } catch (ConcurrentUpdateException lost) {
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
        assertThat(results).containsExactlyInAnyOrder("accepted", "conflict");
        assertThat(List.of(status(first), status(second)))
                .containsExactlyInAnyOrder(BookingStatus.ACCEPTED, BookingStatus.PENDING);
        assertThat(publications.findById(publication.getId()).orElseThrow().getReservedSpots()).isEqualTo(1);
    }

    private long pendingBooking(Publication publication, UserId client, long slotId) {
        TimeSlot slot = publication.getSlots().stream().filter(s -> s.getId() == slotId).findFirst().orElseThrow();
        return unitOfWork.execute(() -> bookings.save(Booking.request(publication.getId(), client, PROVIDER, slot,
                "Hola", "Entrevista", new SkillCoins(100)))).getId();
    }

    private BookingStatus status(long bookingId) {
        return bookings.findById(bookingId).orElseThrow().getStatus();
    }

    // Hace que las dos aceptaciones lean la publicación antes de que cualquiera guarde: así compiten de verdad.
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
