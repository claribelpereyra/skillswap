package com.uade.skillswap.infrastructure.persistence.publication;

import com.uade.skillswap.infrastructure.persistence.PersistenceTestBase;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JpaRecommendationMetadataQueryTest extends PersistenceTestBase {

    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");

    private final JpaRecommendationMetadataQuery query = new JpaRecommendationMetadataQuery();

    @BeforeEach
    void publicationsAndSlots() {
        sql("INSERT INTO publication (id, author_id, type, status, capacity, created_at, version)"
                + " VALUES (1, 'author', 'MENTORING', 'PUBLISHED', 1, ?, 0)", NOW);
        sql("INSERT INTO publication (id, author_id, type, status, capacity, created_at, version)"
                + " VALUES (2, 'author', 'MENTORING', 'PUBLISHED', 1, ?, 0)", NOW);
        sql("INSERT INTO publication_slot (id, publication_id, starts_at, ends_at, occupied)"
                + " VALUES (1, 1, ?, ?, FALSE)", NOW, NOW.plusSeconds(3600));
        sql("INSERT INTO publication_slot (id, publication_id, starts_at, ends_at, occupied)"
                + " VALUES (2, 2, ?, ?, FALSE)", NOW, NOW.plusSeconds(3600));
    }

    @Test
    void skillsAndInterestsAreLoadedForOnlyTheActiveProfile() {
        Assumptions.assumeTrue(profileSchemaIsAvailable(),
                "The profile metadata test requires the Flyway profile migration from PR #37");
        sql("INSERT INTO category (id, parent_id, name, kind) VALUES (1, NULL, 'Java', 'SKILL')");
        sql("INSERT INTO category (id, parent_id, name, kind) VALUES (2, NULL, 'Python', 'SKILL')");
        long activeProfile = profile("ana", true);
        long inactiveProfile = profile("bruno", false);
        sql("INSERT INTO profile_skill (profile_id, skill_id) VALUES (?, ?)", activeProfile, 1L);
        sql("INSERT INTO profile_interest (profile_id, interest) VALUES (?, ?)", activeProfile, "Cloud");
        sql("INSERT INTO profile_skill (profile_id, skill_id) VALUES (?, ?)", inactiveProfile, 2L);

        assertThat(query.skillsAndInterestsFor("ana")).containsExactlyInAnyOrder("java", "cloud");
        assertThat(query.skillsAndInterestsFor("bruno")).isEmpty();
        assertThat(query.skillsAndInterestsFor(null)).isEmpty();
    }

    private boolean profileSchemaIsAvailable() {
        return inTransaction(em -> ((Number) em.createNativeQuery("""
                        SELECT COUNT(*) FROM information_schema.tables
                        WHERE table_schema = CURRENT_SCHEMA
                          AND table_name IN ('profile', 'profile_skill', 'profile_interest')
                        """)
                .getSingleResult()).intValue() == 3);
    }

    @Test
    void countsOnlyRecentAcceptedBookingsInOneBatch() {
        addBooking(1L, "CONFIRMED", NOW.minusSeconds(5 * 24 * 60 * 60L));
        addBooking(1L, "ACCEPTED", NOW.minusSeconds(2 * 24 * 60 * 60L));
        addBooking(1L, "PENDING", NOW.minusSeconds(2 * 24 * 60 * 60L));
        addBooking(2L, "COMPLETED", NOW.minusSeconds(31 * 24 * 60 * 60L));

        assertThat(query.recentBookingCounts(Set.of(1L, 2L), NOW.minusSeconds(30 * 24 * 60 * 60L)))
                .isEqualTo(Map.of(1L, 2L));
        assertThat(query.recentBookingCounts(Set.of(), NOW)).isEmpty();
    }

    private long profile(String userId, boolean active) {
        return inTransaction(em -> {
            em.createNativeQuery("INSERT INTO profile (user_id, display_name, time_zone, account_type, active, created_at)"
                            + " VALUES (?, ?, 'UTC', 'PROFESSIONAL', ?, ?)")
                    .setParameter(1, userId)
                    .setParameter(2, userId)
                    .setParameter(3, active)
                    .setParameter(4, NOW)
                    .executeUpdate();
            return ((Number) em.createNativeQuery("SELECT id FROM profile WHERE user_id = ?")
                    .setParameter(1, userId)
                    .getSingleResult()).longValue();
        });
    }

    private void addBooking(long publicationId, String status, Instant createdAt) {
        long bookingId = inTransaction(em -> {
            em.createNativeQuery("INSERT INTO booking (publication_id, client_id, provider_id, slot_id,"
                            + " slot_starts_at, slot_ends_at, message, goal, price, status, version)"
                            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                    .setParameter(1, publicationId)
                    .setParameter(2, "client")
                    .setParameter(3, "provider")
                    .setParameter(4, publicationId)
                    .setParameter(5, NOW)
                    .setParameter(6, NOW.plusSeconds(3600))
                    .setParameter(7, "message")
                    .setParameter(8, "goal")
                    .setParameter(9, 100L)
                    .setParameter(10, status)
                    .setParameter(11, 0L)
                    .executeUpdate();
            return ((Number) em.createNativeQuery("SELECT MAX(id) FROM booking").getSingleResult()).longValue();
        });
        sql("INSERT INTO booking_transition (booking_id, position, from_status, to_status, actor_id, occurred_at)"
                + " VALUES (?, 0, NULL, 'PENDING', ?, ?)", bookingId, "client", createdAt);
        if (!"PENDING".equals(status)) {
            sql("INSERT INTO booking_transition (booking_id, position, from_status, to_status, actor_id, occurred_at)"
                    + " VALUES (?, 1, 'PENDING', ?, ?, ?)", bookingId, status, "provider", NOW);
        }
    }
}
