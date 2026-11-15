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
import com.uade.skillswap.infrastructure.persistence.PersistenceTestBase;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JpaBookingRepositoryTest extends PersistenceTestBase {

    private static final UserId CLIENT = new UserId("ana");
    private static final UserId PROVIDER = new UserId("bruno");
    private static final TimeSlot SLOT = new TimeSlot(1L,
            Instant.parse("2026-11-19T21:00:00Z"), Instant.parse("2026-11-19T22:00:00Z"));
    private static final TimeSlot OTHER_SLOT = new TimeSlot(2L,
            Instant.parse("2026-11-20T13:00:00Z"), Instant.parse("2026-11-20T14:00:00Z"));

    private final JpaBookingRepository repository = new JpaBookingRepository();

    // Las claves foráneas exigen que la publicación y sus franjas existan.
    @BeforeEach
    void publicationWithItsSlots() {
        sql("INSERT INTO publication (id, author_id, type, status, capacity, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                10L, PROVIDER.value(), "MENTORING", "PUBLISHED", 2, SLOT.getStartsAt());
        sql("INSERT INTO publication_slot (id, publication_id, starts_at, ends_at) VALUES (?, ?, ?, ?)",
                1L, 10L, SLOT.getStartsAt(), SLOT.getEndsAt());
        sql("INSERT INTO publication_slot (id, publication_id, starts_at, ends_at) VALUES (?, ?, ?, ?)",
                2L, 10L, OTHER_SLOT.getStartsAt(), OTHER_SLOT.getEndsAt());
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

    @Test
    void theDatabaseRejectsABookingOfAnUnknownPublication() {
        Booking orphan = Booking.request(99L, CLIENT, PROVIDER, SLOT, "Hola", "Entrevista", new SkillCoins(100));

        assertThatThrownBy(() -> unitOfWork.execute(() -> repository.save(orphan)))
                .isInstanceOf(PersistenceException.class);
        assertThat(count("booking")).isZero();
    }

    @Test
    void theDatabaseRejectsAnUnknownStatusOrANegativePrice() {
        assertThatThrownBy(() -> insertRaw("FINISHED", 100)).isInstanceOf(PersistenceException.class);
        assertThatThrownBy(() -> insertRaw("PENDING", -1)).isInstanceOf(PersistenceException.class);
    }

    private void insertRaw(String status, long price) {
        unitOfWork.execute(() -> EntityManagerContext.withEntityManager(em -> em.createNativeQuery("""
                        INSERT INTO booking (publication_id, client_id, provider_id, slot_id, slot_starts_at, slot_ends_at,
                                             message, goal, price, status, version)
                        VALUES (10, 'ana', 'bruno', 1, ?1, ?2, 'Hola', 'Entrevista', ?3, ?4, 0)
                        """)
                .setParameter(1, SLOT.getStartsAt())
                .setParameter(2, SLOT.getEndsAt())
                .setParameter(3, price)
                .setParameter(4, status)
                .executeUpdate()));
    }

    private static Booking newBooking() {
        return Booking.request(10L, CLIENT, PROVIDER, SLOT, "Hola Bruno", "Preparar una entrevista", new SkillCoins(100));
    }
}
