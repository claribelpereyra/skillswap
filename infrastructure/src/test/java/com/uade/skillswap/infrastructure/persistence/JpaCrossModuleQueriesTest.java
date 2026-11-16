package com.uade.skillswap.infrastructure.persistence;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.publication.PublicationView;
import com.uade.skillswap.application.ports.out.rating.EligibleBookingQuery.BookingTarget;
import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.AccountType;
import com.uade.skillswap.domain.publication.Modality;
import com.uade.skillswap.domain.publication.Publication;
import com.uade.skillswap.domain.publication.PublicationDetails;
import com.uade.skillswap.domain.publication.PublicationType;
import com.uade.skillswap.domain.publication.TimeSlot;
import com.uade.skillswap.infrastructure.persistence.booking.JpaBookingRepository;
import com.uade.skillswap.infrastructure.persistence.profile.JpaActivePublicationsQuery;
import com.uade.skillswap.infrastructure.persistence.publication.JpaPublicationRepository;
import com.uade.skillswap.infrastructure.persistence.rating.JpaEligibleBookingQuery;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JpaCrossModuleQueriesTest extends PersistenceTestBase {

    private static final UserId ANA = new UserId("ana");
    private static final UserId BRUNO = new UserId("bruno");
    private static final UserId CARLA = new UserId("carla");
    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");
    private static final Instant SLOT_START = Instant.parse("2026-11-20T15:00:00Z");

    private final JpaPublicationRepository publicationRepository = new JpaPublicationRepository();
    private final JpaBookingRepository bookingRepository = new JpaBookingRepository();

    @BeforeEach
    void seedCategory() {
        sql("INSERT INTO category (id, parent_id, name, kind) VALUES (?, ?, ?, ?)", 1, null, "Tecnologia", "CATEGORY");
    }

    @Test
    void queriesPublishedPublicationsAndBookingEligibilityThroughSharedRepositories() {
        Publication firstPublished = createPublishedPublication("Java Mentoring");
        Publication pausedPublication = createPublishedPublication("Paused Session");
        pausedPublication.pause(ANA, NOW.plusSeconds(10));
        Publication secondPublished = createPublishedPublication("Testing Workshop");

        Publication savedFirst = savePublication(firstPublished);
        savePublication(pausedPublication);
        Publication savedSecond = savePublication(secondPublished);

        Booking completedBooking = Booking.request(savedFirst.getId(), ANA, BRUNO,
                savedFirst.getSlots().get(0),
                "Hola Bruno", "Mentoria", new SkillCoins(100));
        completedBooking.accept(BRUNO);
        completedBooking.confirmPayment(ANA);
        completedBooking.start(BRUNO);
        completedBooking.complete(ANA);
        Booking savedCompleted = unitOfWork.execute(() -> bookingRepository.save(completedBooking));

        Booking confirmedBooking = Booking.request(savedSecond.getId(), ANA, CARLA,
                savedSecond.getSlots().get(0),
                "Hola Carla", "Code review", new SkillCoins(100));
        confirmedBooking.accept(CARLA);
        confirmedBooking.confirmPayment(ANA);
        Booking savedConfirmed = unitOfWork.execute(() -> bookingRepository.save(confirmedBooking));

        JpaActivePublicationsQuery publicationsQuery = new JpaActivePublicationsQuery(publicationRepository);
        JpaEligibleBookingQuery bookingQuery = new JpaEligibleBookingQuery(bookingRepository);

        Page<PublicationView> anaPage = publicationsQuery.findActiveByUserId(ANA, PageRequest.first());
        assertThat(anaPage.totalItems()).isEqualTo(2L);
        assertThat(anaPage.items()).extracting(PublicationView::title)
                .containsExactlyInAnyOrder("Java Mentoring", "Testing Workshop");
        assertThat(publicationsQuery.findActiveByUserId(BRUNO, PageRequest.first()).items()).isEmpty();
        assertThat(publicationsQuery.findActiveByUserId(null, PageRequest.first()).items()).isEmpty();

        Optional<BookingTarget> completed = bookingQuery.findBooking(savedCompleted.getId());
        assertThat(completed).contains(new BookingTarget(savedCompleted.getId(), ANA, BRUNO, true));

        Optional<BookingTarget> confirmed = bookingQuery.findBooking(savedConfirmed.getId());
        assertThat(confirmed).contains(new BookingTarget(savedConfirmed.getId(), ANA, CARLA, false));

        assertThat(bookingQuery.findBooking(9999L)).isEmpty();
    }

    @Test
    void demoSeedMigrationLoadsCoherentSchemaDataWithoutBlockingFutureMigrations() {
        String demoUrl = "jdbc:h2:mem:demo-seed-check;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway flyway = Flyway.configure()
                .dataSource(demoUrl, "sa", "")
                .locations("classpath:db/migration", "classpath:db/seed")
                .load();
        flyway.migrate();
        flyway.migrate();

        PersistenceConfiguration demoDb = new PersistenceConfiguration(demoUrl, "sa", "");
        try (var emf = demoDb.createEntityManagerFactory(); var em = emf.createEntityManager()) {
            long profiles = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM profile").getSingleResult())
                    .longValue();
            long ratings = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM rating").getSingleResult())
                    .longValue();
            double anaReputation = ((Number) em.createNativeQuery(
                    "SELECT reputation FROM profile WHERE user_id = 'ana'").getSingleResult()).doubleValue();
            assertThat(profiles).isEqualTo(3L);
            assertThat(ratings).isEqualTo(2L);
            assertThat(anaReputation).isEqualTo(5.0);
        }
    }

    private Publication savePublication(Publication publication) {
        return unitOfWork.execute(() -> publicationRepository.save(publication));
    }

    private static Publication createPublishedPublication(String title) {
        PublicationDetails details = new PublicationDetails(
                title, "Practical mentoring", 1L, Modality.REMOTE, Duration.ofMinutes(60),
                2, new SkillCoins(100), Set.of("java"));
        Publication publication = Publication.draft(ANA, AccountType.PROFESSIONAL, PublicationType.MENTORING, details, NOW);
        publication.addSlot(ANA, SLOT_START, SLOT_START.plus(Duration.ofHours(1)));
        publication.publish(ANA, NOW.plusSeconds(1));
        return publication;
    }
}
