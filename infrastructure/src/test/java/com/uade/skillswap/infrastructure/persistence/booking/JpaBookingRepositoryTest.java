package com.uade.skillswap.infrastructure.persistence.booking;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.StateTransition;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.TimeSlot;
import com.uade.skillswap.infrastructure.persistence.EntityManagerContext;
import com.uade.skillswap.infrastructure.persistence.JpaUnitOfWork;
import com.uade.skillswap.infrastructure.persistence.PersistenceConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JpaBookingRepositoryTest {

    private static final UserId CLIENT = new UserId("ana");
    private static final UserId PROVIDER = new UserId("bruno");
    private static final TimeSlot SLOT = new TimeSlot(1L,
            Instant.parse("2026-11-19T21:00:00Z"), Instant.parse("2026-11-19T22:00:00Z"));
    private static final TimeSlot OTHER_SLOT = new TimeSlot(2L,
            Instant.parse("2026-11-20T13:00:00Z"), Instant.parse("2026-11-20T14:00:00Z"));

    private final JpaBookingRepository repository = new JpaBookingRepository();
    private final JpaUnitOfWork unitOfWork = new JpaUnitOfWork();

    @BeforeAll
    static void startDatabase() {
        PersistenceConfiguration h2 = new PersistenceConfiguration(
                "jdbc:h2:mem:bookings;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        h2.migrate();
        EntityManagerContext.init(h2.createEntityManagerFactory());
    }

    @AfterAll
    static void stopDatabase() {
        EntityManagerContext.close();
    }

    @BeforeEach
    void cleanTables() {
        unitOfWork.execute(() -> EntityManagerContext.withEntityManager(em -> {
            em.createNativeQuery("DELETE FROM booking_transition").executeUpdate();
            return em.createNativeQuery("DELETE FROM booking").executeUpdate();
        }));
    }

    @Test
    void savesANewBookingAndReadsItBackTheSame() {
        Booking saved = unitOfWork.execute(() -> repository.save(newBooking()));

        Booking read = repository.findById(saved.getId()).orElseThrow();

        assertThat(saved.getId()).isNotNull();
        assertThat(read.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(read.getClientId()).isEqualTo(CLIENT);
        assertThat(read.getProviderId()).isEqualTo(PROVIDER);
        assertThat(read.getSlot().getId()).isEqualTo(1L);
        assertThat(read.getSlot().getStartsAt()).isEqualTo(SLOT.getStartsAt());
        assertThat(read.getPrice()).isEqualTo(new SkillCoins(100));
        assertThat(read.getMessage()).isEqualTo("Hola Bruno");
        assertThat(read.getGoal()).isEqualTo("Preparar una entrevista");
        assertThat(read.getHistory()).singleElement()
                .extracting(StateTransition::actor).isEqualTo(CLIENT);
    }

    @Test
    void savesStateChangesAndAppendsTheHistory() {
        Long id = unitOfWork.execute(() -> repository.save(newBooking())).getId();

        unitOfWork.execute(() -> {
            Booking booking = repository.findById(id).orElseThrow();
            booking.accept(PROVIDER);
            booking.confirmPayment(CLIENT);
            return repository.save(booking);
        });

        Booking read = repository.findById(id).orElseThrow();
        assertThat(read.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(read.getHistory()).extracting(StateTransition::to)
                .containsExactly(BookingStatus.PENDING, BookingStatus.ACCEPTED, BookingStatus.CONFIRMED);
        assertThat(read.getHistory()).extracting(StateTransition::actor)
                .containsExactly(CLIENT, PROVIDER, CLIENT);
    }

    @Test
    void savesAndClearsAPendingReschedule() {
        Long id = unitOfWork.execute(() -> {
            Booking booking = newBooking();
            booking.accept(PROVIDER);
            booking.proposeReschedule(PROVIDER, OTHER_SLOT);
            return repository.save(booking);
        }).getId();

        Booking withProposal = repository.findById(id).orElseThrow();
        assertThat(withProposal.getPendingReschedule()).hasValueSatisfying(proposal -> {
            assertThat(proposal.newSlot().getId()).isEqualTo(2L);
            assertThat(proposal.newSlot().getEndsAt()).isEqualTo(OTHER_SLOT.getEndsAt());
            assertThat(proposal.proposedBy()).isEqualTo(PROVIDER);
        });

        unitOfWork.execute(() -> {
            Booking booking = repository.findById(id).orElseThrow();
            booking.acceptReschedule(CLIENT);
            return repository.save(booking);
        });

        Booking rescheduled = repository.findById(id).orElseThrow();
        assertThat(rescheduled.getPendingReschedule()).isEmpty();
        assertThat(rescheduled.getSlot().getId()).isEqualTo(2L);
        assertThat(rescheduled.getHistory().get(rescheduled.getHistory().size() - 1).reason())
                .isEqualTo("Rescheduled to slot 2");
    }

    @Test
    void returnsEmptyForAnUnknownBooking() {
        assertThat(repository.findById(12345L)).isEmpty();
    }

    @Test
    void listsTheBookingsOfAClientAndOfAProviderPaginated() {
        unitOfWork.execute(() -> {
            repository.save(newBooking());
            repository.save(newBooking());
            return repository.save(newBooking());
        });

        Page<Booking> firstPage = repository.findByClient(CLIENT, new PageRequest(0, 2));
        Page<Booking> asProvider = repository.findByProvider(PROVIDER, new PageRequest(1, 2));

        assertThat(firstPage.items()).hasSize(2);
        assertThat(firstPage.totalItems()).isEqualTo(3);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.items().get(0).getId()).isGreaterThan(firstPage.items().get(1).getId());
        assertThat(firstPage.items()).allSatisfy(booking -> assertThat(booking.getHistory()).hasSize(1));
        assertThat(asProvider.items()).hasSize(1);
        assertThat(repository.findByClient(PROVIDER, PageRequest.first()).items()).isEmpty();
    }

    private static Booking newBooking() {
        return Booking.request(10L, CLIENT, PROVIDER, SLOT, "Hola Bruno", "Preparar una entrevista", new SkillCoins(100));
    }
}
