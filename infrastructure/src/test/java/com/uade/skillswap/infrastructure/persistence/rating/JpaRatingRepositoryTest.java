package com.uade.skillswap.infrastructure.persistence.rating;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.rating.InvalidRatingException;
import com.uade.skillswap.domain.rating.Rating;
import com.uade.skillswap.infrastructure.persistence.PersistenceTestBase;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JpaRatingRepositoryTest extends PersistenceTestBase {

    private static final UserId ANA = new UserId("ana");
    private static final UserId BRUNO = new UserId("bruno");
    private static final UserId CARLA = new UserId("carla");

    private final JpaRatingRepository repository = new JpaRatingRepository();

    @BeforeEach
    void seedBookings() {
        Instant slotStart = Instant.parse("2026-11-10T10:00:00Z");
        sql("INSERT INTO publication (id, author_id, type, status, capacity, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                1L, "ana", "MENTORING", "PUBLISHED", 2, slotStart);
        sql("INSERT INTO publication_slot (id, publication_id, starts_at, ends_at) VALUES (?, ?, ?, ?)",
                1L, 1L, slotStart, slotStart.plusSeconds(3600));
        insertBooking(1L, "bruno", "ana");
        insertBooking(2L, "carla", "ana");
        insertBooking(3L, "carla", "ana");
        insertBooking(9L, "ana", "bruno");
    }

    @Test
    void savesQueriesAndPaginatesReceivedRatingsNewestFirst() {
        Rating r1 = Rating.create(1L, BRUNO, ANA, 4, "Older", Instant.parse("2026-11-01T10:00:00Z"));
        Rating r2 = Rating.create(2L, CARLA, ANA, 5, "Newer", Instant.parse("2026-11-05T10:00:00Z"));
        Rating r3 = Rating.create(3L, CARLA, ANA, 5, "Newest", Instant.parse("2026-11-08T10:00:00Z"));

        unitOfWork.execute(() -> {
            repository.save(r1);
            repository.save(r2);
            repository.save(r3);
        });

        assertThat(repository.existsByBookingIdAndAuthorId(2L, CARLA)).isTrue();
        assertThat(repository.existsByBookingIdAndAuthorId(2L, ANA)).isFalse();
        assertThat(repository.existsByBookingIdAndAuthorId(2L, null)).isFalse();

        Page<Rating> page0 = repository.findByRecipientId(ANA, new PageRequest(0, 2));
        assertThat(page0.totalItems()).isEqualTo(3L);
        assertThat(page0.totalPages()).isEqualTo(2);
        assertThat(page0.items()).extracting(Rating::getComment).containsExactly("Newest", "Newer");

        Page<Rating> page1 = repository.findByRecipientId(ANA, new PageRequest(1, 2));
        assertThat(page1.items()).extracting(Rating::getComment).containsExactly("Older");

        Page<Rating> emptyUserPage = repository.findByRecipientId(CARLA, PageRequest.first());
        assertThat(emptyUserPage.items()).isEmpty();
        assertThat(emptyUserPage.totalItems()).isZero();
    }

    @Test
    void translatesDuplicateBookingAndAuthorIntoInvalidRatingException() {
        unitOfWork.execute(() -> repository.save(Rating.create(9L, ANA, BRUNO, 5, "Great")));

        assertThatThrownBy(() -> unitOfWork.execute(
                () -> repository.save(Rating.create(9L, ANA, BRUNO, 3, "Duplicate"))))
                .isExactlyInstanceOf(InvalidRatingException.class)
                .hasMessage("Booking 9 has already been rated by user ana");
        assertThat(count("rating")).isEqualTo(1L);
    }

    @Test
    void databaseEnforcesBookingForeignKeyAndSelfRatingCheck() {
        assertThatThrownBy(() -> unitOfWork.execute(
                () -> repository.save(Rating.create(999L, ANA, BRUNO, 5, "Missing booking"))))
                .isInstanceOf(PersistenceException.class);

        assertThatThrownBy(() -> sql("""
                INSERT INTO rating (booking_id, author_id, recipient_id, score, comment, rated_at)
                VALUES (9, 'ana', 'ana', 5, 'Self', '2026-11-10 10:00:00')
                """))
                .isInstanceOf(PersistenceException.class);
        assertThat(count("rating")).isZero();
    }

    private void insertBooking(long id, String clientId, String providerId) {
        sql("""
                INSERT INTO booking (id, publication_id, client_id, provider_id, slot_id,
                                     slot_starts_at, slot_ends_at, message, goal, price, status, version)
                VALUES (?, 1, ?, ?, 1, '2026-11-10 10:00:00', '2026-11-10 11:00:00',
                        'Hola', 'Mentoria', 100, 'COMPLETED', 0)
                """, id, clientId, providerId);
    }
}
